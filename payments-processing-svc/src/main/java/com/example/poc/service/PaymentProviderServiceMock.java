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
import com.example.poc.common.dto.AckPaymentSentDto;
import com.example.poc.common.dto.PaymentStatusDto;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentStatusMapper;
import com.example.poc.common.mapper.SendPaymentRequestMapper;
import com.google.common.util.concurrent.RateLimiter;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

@SuppressWarnings("UnstableApiUsage")
@ApplicationScoped
public class PaymentProviderServiceMock implements PaymentProviderService {

  private final RateLimiter rateLimiter;
  private final long timeoutMillis;
  private final AckPaymentSentMapper ackPaymentSentMapper;
  private final PaymentStatusMapper paymentStatusMapper;

  @Inject
  public PaymentProviderServiceMock(
      AckPaymentSentMapper ackPaymentSentMapper,
      PaymentStatusMapper paymentStatusMapper,
      PaymentProviderConfig config) {
    this.ackPaymentSentMapper = ackPaymentSentMapper;
    this.paymentStatusMapper = paymentStatusMapper;
    rateLimiter = RateLimiter.create(config.permitsPerSecond());
    timeoutMillis = config.timeoutMillis();

    System.out.println(
        "PaymentProviderConfig loaded: permitsPerSecond="
            + config.permitsPerSecond()
            + ", "
            + "timeoutMillis="
            + config.timeoutMillis());
  }

  @Override
  public AckPaymentSent sendPayment(
      @NonNull SendPaymentRequestMapper.SendPaymentRequest requestMap) {
    // Try to acquire with timeout
    if (this.timeoutMillis == -1L
        || !rateLimiter.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
      throw new StatusRuntimeException(
          Status.RESOURCE_EXHAUSTED.withDescription(
              "Payment service is currently throttled. Please try again later."));
    }

    return ackPaymentSentMapper.fromDto(
        AckPaymentSentDto.builder()
            .id(UUID.randomUUID())
            .status(1000L)
            .message("OK but this is only a test")
            .conversationId(UUID.randomUUID())
            .paymentRecordId(requestMap.getPaymentRecordId())
            .paymentRecord(requestMap.getPaymentRecord())
            .build());
  }

  @Override
  public PaymentStatus getPaymentStatus(@NonNull AckPaymentSent ackPaymentSent) {
    // Try to acquire with timeout
    if (this.timeoutMillis == -1L
        || !rateLimiter.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
      throw new StatusRuntimeException(
          Status.RESOURCE_EXHAUSTED.withDescription(
              "Failed to acquire permit within timeout period. The payment status service is currently throttled."));
    }

    return paymentStatusMapper.fromDto(
        PaymentStatusDto.builder()
            .id(UUID.randomUUID())
            .reference("101")
            .status("Complete")
            .fee(new BigDecimal("1.01"))
            .message("Mock response")
            .ackPaymentSent(ackPaymentSent)
            .ackPaymentSentId(ackPaymentSent.getId())
            .build());
  }
}
