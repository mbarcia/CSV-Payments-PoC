/*
 * Copyright (c) 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pipelineframework.csv.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.time.Duration;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.pipelineframework.csv.common.domain.AckPaymentSent;
import org.pipelineframework.csv.common.domain.PaymentStatus;

@ApplicationScoped
@Alternative
@Priority(1)
public class PollAckPaymentSentReactiveService
    implements PollAckPaymentSentService<AckPaymentSent, PaymentStatus> {

  private final Logger logger =
      Logger.getLogger(getClass());
  private final PaymentProviderService paymentProviderServiceMock;
  private final PaymentProviderConfig config;

  @Inject io.vertx.mutiny.core.Vertx vertx;

  @Inject
  public PollAckPaymentSentReactiveService(
      PaymentProviderService paymentProviderServiceMock,
      PaymentProviderConfig config) {
	    this.paymentProviderServiceMock = paymentProviderServiceMock;
        this.config = config;
        logger.debugf(
            "PollAckPaymentSentReactiveService initialized with config: permitsPerSecond=%s, timeoutMillis=%s, waitMilliseconds=%s",
            config.permitsPerSecond(),
            config.timeoutMillis(),
            config.waitMilliseconds());
  }

  @Override
  public Uni<PaymentStatus> process(AckPaymentSent detachedAckPaymentSent) {
      logger.debugf(
          "Processing AckPaymentSent: id=%s, conversationId=%s, paymentRecordId=%s",
          detachedAckPaymentSent.getId(),
          detachedAckPaymentSent.getConversationId(),
          detachedAckPaymentSent.getPaymentRecordId());

      return Uni.createFrom()
          .item(detachedAckPaymentSent)
          // ---- IMPORTANT! Offload the entire chain ----
          .runSubscriptionOn(Infrastructure.getDefaultExecutor())
          // ----------------------------------------------
          .onItem()
          .transformToUni(ack -> {
            long time = (long) (Math.random() * config.waitMilliseconds() + 1);
            logger.infof("Started polling...(for %sms)", time);
            logger.debugf("Thread: %s isVirtual? %s", Thread.currentThread(), Thread.currentThread().isVirtual());

            logger.debugf("About to delay for %sms", time);

            // Use reactive delay instead of Thread.sleep
            return Uni.createFrom().item(ack)
                .onItem().delayIt().by(Duration.ofMillis(time))
                .onItem().transformToUni(_ ->
                    // Wrap blocking service call with vertx.executeBlocking
                    vertx.executeBlocking(() -> {
                      logger.debug("Calling paymentProviderServiceMock.getPaymentStatus");
                      return paymentProviderServiceMock.getPaymentStatus(ack);
                    })
                )
                .onItem().invoke(result -> {
                  logger.debugf(
                      "Received PaymentStatus: id=%s, reference=%s, status=%s",
                      result.getId(),
                      result.getReference(),
                      result.getStatus());

                  String serviceId = this.getClass().toString();
                  MDC.put("serviceId", serviceId);
                  logger.infof("Executed command on %s --> %s", detachedAckPaymentSent, result);
                  MDC.remove("serviceId");
                });
          })
          .onFailure().invoke(failure -> logger.error("Failed to process AckPaymentSent", failure));
  }

}
