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

package io.github.mbarcia.csv.service;

import com.google.common.util.concurrent.RateLimiter;
import io.github.mbarcia.csv.common.domain.AckPaymentSent;
import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.common.dto.AckPaymentSentDto;
import io.github.mbarcia.csv.common.dto.PaymentStatusDto;
import io.github.mbarcia.csv.common.mapper.AckPaymentSentMapper;
import io.github.mbarcia.csv.common.mapper.PaymentStatusMapper;
import io.github.mbarcia.csv.common.mapper.SendPaymentRequestMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
@ApplicationScoped
public class PaymentProviderServiceMock implements PaymentProviderService {
    
  private static final Logger LOG = LoggerFactory.getLogger(PaymentProviderServiceMock.class);

  private final RateLimiter rateLimiter;
  private final long timeoutMillis;
  private final AckPaymentSentMapper ackPaymentSentMapper = AckPaymentSentMapper.INSTANCE;
  private final PaymentStatusMapper paymentStatusMapper = PaymentStatusMapper.INSTANCE;

  @Inject
  public PaymentProviderServiceMock(PaymentProviderConfig config) {
    rateLimiter = RateLimiter.create(config.permitsPerSecond());
    timeoutMillis = config.timeoutMillis();

    LOG.info(
        "PaymentProviderServiceMock initialized: permitsPerSecond={}, timeoutMillis={}",
        config.permitsPerSecond(),
        config.timeoutMillis());
  }

  @Override
  public AckPaymentSent sendPayment(
      @NonNull SendPaymentRequestMapper.SendPaymentRequest requestMap) {
    LOG.debug("sendPayment called with request: amount={}, currency={}, reference={}, paymentRecordId={}",
        requestMap.getAmount(), requestMap.getCurrency(), requestMap.getReference(), requestMap.getPaymentRecordId());
        
    // Try to acquire with timeout
    LOG.debug("Attempting to acquire rate limiter permit with timeout: {}ms", timeoutMillis);
    boolean acquired = (this.timeoutMillis != -1L && rateLimiter.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS));
    
    if (!acquired) {
      LOG.debug("Failed to acquire rate limiter permit within timeout period: {}ms", timeoutMillis);
      throw new StatusRuntimeException(
          Status.RESOURCE_EXHAUSTED.withDescription(
              "Payment service is currently throttled. Please try again later."));
    }
    
    LOG.debug("Rate limiter permit acquired successfully");

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
    LOG.debug("getPaymentStatus called with AckPaymentSent: id={}, conversationId={}, paymentRecordId={}", 
        ackPaymentSent.getId(), ackPaymentSent.getConversationId(), ackPaymentSent.getPaymentRecordId());
        
    // Try to acquire with timeout
    LOG.debug("Attempting to acquire rate limiter permit with timeout: {}ms", timeoutMillis);
    boolean acquired = (this.timeoutMillis != -1L && rateLimiter.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS));
    
    if (!acquired) {
      LOG.warn("Failed to acquire rate limiter permit within timeout period: {}ms", timeoutMillis);
      throw new StatusRuntimeException(
          Status.RESOURCE_EXHAUSTED.withDescription(
              "Failed to acquire permit within timeout period. The payment status service is currently throttled."));
    }
    
    LOG.debug("Rate limiter permit acquired successfully");

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
