/*
 * Copyright © 2023-2025 Mariano Barcia
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

package com.example.poc.service;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.service.ReactiveService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
public class PollAckPaymentSentReactiveService
    implements ReactiveService<AckPaymentSent, PaymentStatus> {

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
  }

  @Override
  public Uni<PaymentStatus> process(AckPaymentSent detachedAckPaymentSent) {
    Logger logger = LoggerFactory.getLogger(this.getClass());

      return Uni.createFrom()
        .item(detachedAckPaymentSent)
        .runSubscriptionOn(executor)
        .map(
            ack -> {
              try {
                long time = (long) (Math.random() * config.waitMilliseconds());
                logger.info("Started polling...(for {}ms)", time);
                logger.info(
                    "Thread: {} isVirtual? {}",
                    Thread.currentThread(),
                    Thread.currentThread().isVirtual());
                Thread.sleep(time); // simulate delay
                logger.info("Finished polling (--> {}ms)", time);

                PaymentStatus result = paymentProviderServiceMock.getPaymentStatus(ack);

                String serviceId = this.getClass().toString();
                MDC.put("serviceId", serviceId);
                logger.info("Executed command on {} --> {}", detachedAckPaymentSent, result);
                MDC.clear();

                return result;
              } catch (JsonProcessingException | InterruptedException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
