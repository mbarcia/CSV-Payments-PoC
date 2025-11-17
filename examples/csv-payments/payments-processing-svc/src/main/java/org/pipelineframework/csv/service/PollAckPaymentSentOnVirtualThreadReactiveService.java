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
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.pipelineframework.csv.common.domain.AckPaymentSent;
import org.pipelineframework.csv.common.domain.PaymentStatus;

@ApplicationScoped
@Alternative
@Priority(1)
public class PollAckPaymentSentOnVirtualThreadReactiveService
    implements PollAckPaymentSentService<AckPaymentSent, PaymentStatus> {

  private final Logger logger =
      Logger.getLogger(getClass());
  private final PaymentProviderService paymentProviderServiceMock;
  private final PaymentProviderConfig config;

  /**
   * Create a service instance that will poll for payment acknowledgements using the supplied provider and configuration.
   *
   * @param paymentProviderServiceMock the payment provider service used to retrieve payment status
   * @param config the configuration controlling polling behaviour and timeouts
   */
  @Inject
  public PollAckPaymentSentOnVirtualThreadReactiveService(
      PaymentProviderService paymentProviderServiceMock,
      PaymentProviderConfig config) {
	    this.paymentProviderServiceMock = paymentProviderServiceMock;
        this.config = config;
        logger.debugf(
            "PollAckPaymentSentOnVirtualThreadReactiveService initialized with config: permitsPerSecond=%s, timeoutMillis=%s, waitMilliseconds=%s",
            config.permitsPerSecond(),
            config.timeoutMillis(),
            config.waitMilliseconds());
  }

    /**
     * Processes an AckPaymentSent by polling the payment provider and returning the resulting payment status.
     *
     * Simulates a polling delay up to the configured wait period, invokes the payment provider to obtain the
     * PaymentStatus for the given AckPaymentSent, and propagates any processing failures through the returned Uni.
     *
     * @param detachedAckPaymentSent the AckPaymentSent to query status for
     * @return the resulting PaymentStatus for the provided AckPaymentSent
     */
    @Override
    public Uni<PaymentStatus> process(AckPaymentSent detachedAckPaymentSent) {
        logger.debugf(
                "Processing AckPaymentSent: id=%s, conversationId=%s, paymentRecordId=%s",
                detachedAckPaymentSent.getId(),
                detachedAckPaymentSent.getConversationId(),
                detachedAckPaymentSent.getPaymentRecordId()
        );

        return Uni.createFrom()
                .item(() -> {
                    long time = (long) (Math.random() * config.waitMilliseconds() + 1);
                    logger.infof("Started polling...(for %sms)", time);
                    logger.debugf("Thread: %s isVirtual? %s",
                            Thread.currentThread(), Thread.currentThread().isVirtual());

                    logger.debugf("About to delay for %dms", time);

                    // Safe on virtual threads (no event-loop hop)
                    logger.debugf("About to sleep for %sms", time);
                    try {
                        Thread.sleep(time); // simulate delay
                        logger.infof("Finished polling (--> %dms)", time);
                    } catch (InterruptedException e) {
                        logger.error("InterruptedException while sleeping: %s", e.getMessage(), e);
                        Thread.currentThread().interrupt(); // Restore interrupt status
                        throw new RuntimeException("InterruptedException while sleeping", e);
                    }

                    logger.debug("Calling paymentProviderServiceMock.getPaymentStatus");
                    PaymentStatus result =
                            paymentProviderServiceMock.getPaymentStatus(detachedAckPaymentSent);

                    logger.debugf(
                            "Received PaymentStatus: id=%s, reference=%s, status=%s",
                            result.getId(),
                            result.getReference(),
                            result.getStatus()
                    );

                    String serviceId = this.getClass().toString();
                    MDC.put("serviceId", serviceId);
                    logger.infof("Executed command on %s --> %s", detachedAckPaymentSent, result);
                    MDC.remove("serviceId");

                    return result;
                })
                .onFailure()
                .invoke(failure ->
                        logger.error("Failed to process AckPaymentSent", failure)
                );
    }
}