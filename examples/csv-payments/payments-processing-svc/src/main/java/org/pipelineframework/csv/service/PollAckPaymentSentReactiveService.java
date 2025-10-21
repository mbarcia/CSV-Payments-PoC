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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.concurrent.Executor;
import org.pipelineframework.csv.common.domain.AckPaymentSent;
import org.pipelineframework.csv.common.domain.PaymentStatus;
import org.pipelineframework.service.ReactiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
public class PollAckPaymentSentReactiveService
    implements ReactiveService<AckPaymentSent, PaymentStatus> {

  private static final Logger LOG = LoggerFactory.getLogger(PollAckPaymentSentReactiveService.class);

  private final Executor executor;
  private final PaymentProviderService paymentProviderServiceMock;
  private final PaymentProviderConfig config;

  @Inject
  public PollAckPaymentSentReactiveService(
      @Named("virtualExecutor") Executor executor,
      PaymentProviderService paymentProviderServiceMock,
      PaymentProviderConfig config) {
    this.executor = executor;
    this.paymentProviderServiceMock = paymentProviderServiceMock;
    this.config = config;
    LOG.debug("PollAckPaymentSentReactiveService initialized with config: permitsPerSecond={}, timeoutMillis={}, waitMilliseconds={}", 
        config.permitsPerSecond(), config.timeoutMillis(), config.waitMilliseconds());
  }

  @Override
  public Uni<PaymentStatus> process(AckPaymentSent detachedAckPaymentSent) {
    return process(detachedAckPaymentSent, true); // Default to using virtual threads
  }

  /**
   * Process with optional virtual thread execution
   * @param detachedAckPaymentSent the payment to process
   * @param useVirtualThreads whether to use virtual threads (false for REST calls)
   * @return Uni with the payment status
   */
  public Uni<PaymentStatus> process(AckPaymentSent detachedAckPaymentSent, boolean useVirtualThreads) {
    LOG.debug("Processing AckPaymentSent: id={}, conversationId={}, paymentRecordId={}", 
        detachedAckPaymentSent.getId(), detachedAckPaymentSent.getConversationId(), detachedAckPaymentSent.getPaymentRecordId());

    if (useVirtualThreads) {
      return processWithVirtualThreads(detachedAckPaymentSent);
    } else {
      return processWithoutVirtualThreads(detachedAckPaymentSent);
    }
  }

  @SuppressWarnings("BlockingMethodInNonBlockingContext")
  private Uni<PaymentStatus> processWithVirtualThreads(AckPaymentSent detachedAckPaymentSent) {
    return Uni.createFrom()
        .item(detachedAckPaymentSent)
        .runSubscriptionOn(executor)
        .onItem()
        .transformToUni(
            ack -> {
              long time = (long) (Math.random() * config.waitMilliseconds());
              LOG.info("Started polling...(for {}ms)", time);
              LOG.debug("Thread: {} isVirtual? {}", Thread.currentThread(), Thread.currentThread().isVirtual());

              // Log before sleep
              LOG.debug("About to sleep for {}ms", time);
              try {
                Thread.sleep(time); // simulate delay. Blocking is not an issue inside a virtual thread.
                LOG.info("Finished polling (--> {}ms)", time);
              } catch (InterruptedException e) {
                LOG.error("InterruptedException while sleeping: {}", e.getMessage(), e);
                Thread.currentThread().interrupt(); // Restore interrupt status
                return Uni.createFrom().failure(new RuntimeException(e));
              }

              return Uni.createFrom().item(() -> {
                LOG.debug("Calling paymentProviderServiceMock.getPaymentStatus");
                return paymentProviderServiceMock.getPaymentStatus(ack);
              })
              .onItem()
              .invoke(result -> {
                LOG.debug("Received PaymentStatus: id={}, reference={}, status={}",
                    result.getId(), result.getReference(), result.getStatus());

                String serviceId = this.getClass().toString();
                MDC.put("serviceId", serviceId);
                LOG.info("Executed command on {} --> {}", detachedAckPaymentSent, result);
                MDC.remove("serviceId");
              })
              .onFailure()
              .recoverWithUni(failure -> {
                LOG.error("Failed to process AckPaymentSent", failure);
                return Uni.createFrom().failure(failure);
              });
            })
        .onFailure()
        .invoke(failure -> LOG.error("Failed to process AckPaymentSent", failure));
  }

  private Uni<PaymentStatus> processWithoutVirtualThreads(AckPaymentSent detachedAckPaymentSent) {
    long time = (long) (Math.random() * config.waitMilliseconds());
    LOG.info("Started polling...(for {}ms)", time);
    LOG.debug("Thread: {} isVirtual? {}", Thread.currentThread(), Thread.currentThread().isVirtual());

    // Simulate delay without virtual threads (this might block the event loop, but it's for REST calls)
    LOG.debug("About to sleep for {}ms", time);
    try {
      Thread.sleep(time); // simulate delay
      LOG.info("Finished polling (--> {}ms)", time);
    } catch (InterruptedException e) {
      LOG.error("InterruptedException while sleeping: {}", e.getMessage(), e);
      Thread.currentThread().interrupt(); // Restore interrupt status
      return Uni.createFrom().failure(new RuntimeException(e));
    }

    return Uni.createFrom().item(() -> {
          LOG.debug("Calling paymentProviderServiceMock.getPaymentStatus");
          return paymentProviderServiceMock.getPaymentStatus(detachedAckPaymentSent);
        })
        .onItem()
        .invoke(result -> {
          LOG.debug("Received PaymentStatus: id={}, reference={}, status={}",
              result.getId(), result.getReference(), result.getStatus());

          String serviceId = this.getClass().toString();
          MDC.put("serviceId", serviceId);
          LOG.info("Executed command on {} --> {}", detachedAckPaymentSent, result);
          MDC.remove("serviceId");
        })
        .onFailure()
        .recoverWithUni(failure -> {
          LOG.error("Failed to process AckPaymentSent", failure);
          return Uni.createFrom().failure(failure);
        });
  }
}
