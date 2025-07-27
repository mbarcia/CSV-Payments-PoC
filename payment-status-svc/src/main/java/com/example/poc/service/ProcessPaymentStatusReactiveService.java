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

import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.service.ReactiveService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ApplicationScoped
@Getter
public class ProcessPaymentStatusReactiveService implements ReactiveService<PaymentStatus, PaymentOutput> {
  @Override
  public Uni<PaymentOutput> process(PaymentStatus paymentStatus) {
    paymentStatus.save();

    PaymentRecord paymentRecord = paymentStatus.getAckPaymentSent().getPaymentRecord();

    return Uni.createFrom()
        .item(
            new PaymentOutput(
                paymentStatus,
                paymentRecord.getCsvId(),
                paymentRecord.getRecipient(),
                paymentRecord.getAmount(),
                paymentRecord.getCurrency(),
                paymentStatus.getAckPaymentSent().getConversationId(),
                paymentStatus.getAckPaymentSent().getStatus(),
                paymentStatus.getMessage(),
                paymentStatus.getFee()))
        .invoke(
            result -> {
              String serviceId = this.getClass().toString();
              Logger logger = LoggerFactory.getLogger(this.getClass());
              MDC.put("serviceId", serviceId);
              logger.info("Executed command on {} --> {}", paymentStatus, result);
              MDC.clear();
            });
  }
}
