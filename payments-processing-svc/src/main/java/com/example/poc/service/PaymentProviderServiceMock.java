package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.mapper.SendPaymentRequestMapper;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
@ApplicationScoped
public class PaymentProviderServiceMock implements PaymentProviderService {

    public static final String UUID = "ac007cbd-1504-4207-8d9f-0abc4b1d2bd8";

    private final RateLimiter rateLimiter;
    private final long timeoutMillis;

    @Inject
    @SuppressWarnings("unused")
    public PaymentProviderServiceMock(PaymentProviderConfig config) {
        rateLimiter = RateLimiter.create(config.permitsPerSecond());
        timeoutMillis = config.timeoutMillis();

        System.out.println("PaymentProviderConfig loaded: permitsPerSecond=" +
                config.permitsPerSecond() + ", timeoutMillis=" + config.timeoutMillis());
    }

    // Constructor for testing with explicit values
    // Setting the timeout to -1 causes sendPayment() to throw an exception
    public PaymentProviderServiceMock(double permitsPerSecond, long timeoutMillis) {
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public AckPaymentSent sendPayment(SendPaymentRequestMapper.SendPaymentRequest requestMap) {
        // Try to acquire with timeout
        if (this.timeoutMillis == -1L || !rateLimiter.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new ThrottlingException("Failed to acquire permit within timeout period. The payment service is currently throttled.");
        }

        return new AckPaymentSent(UUID)
                .setStatus(1000L)
                .setMessage("OK but this is only a test")
                .setPaymentRecord(requestMap.getPaymentRecord())
                .setPaymentRecordId(requestMap.getPaymentRecordId());
    }

    @Override
    public PaymentStatus getPaymentStatus(AckPaymentSent ackPaymentSent) {
        // Try to acquire with timeout
        if (this.timeoutMillis == -1L || !rateLimiter.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new ThrottlingException("Failed to acquire permit within timeout period. The payment status service is currently throttled.");
        }

        return new PaymentStatus("101")
                .setStatus("nada")
                .setFee(new BigDecimal("1.01"))
                .setMessage("This is a test")
                .setAckPaymentSent(ackPaymentSent);
    }

    // Custom exception for throttling
    public static class ThrottlingException extends RuntimeException {
        public ThrottlingException(String message) {
            super(message);
        }
    }
}
