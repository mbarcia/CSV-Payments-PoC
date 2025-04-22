package com.example.poc.service;

import com.example.poc.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
@ApplicationScoped
public class PaymentProviderServiceImpl implements PaymentProviderService {

    public static final String UUID = "ac007cbd-1504-4207-8d9f-0abc4b1d2bd8";

    private final RateLimiter rateLimiter;
    private final long timeoutMillis;

    @Inject
    @SuppressWarnings("unused")
    public PaymentProviderServiceImpl(PaymentProviderConfig config) {
        rateLimiter = RateLimiter.create(config.permitsPerSecond());
        timeoutMillis = config.timeoutMillis();

        System.out.println("PaymentProviderConfig loaded: permitsPerSecond=" +
                config.permitsPerSecond() + ", timeoutMillis=" + config.timeoutMillis());
    }

    // Constructor for testing with explicit values
    // Setting the timeout to -1 causes sendPayment() to throw an exception
    public PaymentProviderServiceImpl(double permitsPerSecond, long timeoutMillis) {
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
        this.timeoutMillis = timeoutMillis;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public AckPaymentSent sendPayment(SendPaymentRequest requestMap) {
        // Try to acquire with timeout
        if (this.timeoutMillis == -1L || !rateLimiter.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new ThrottlingException("Failed to acquire permit within timeout period. The payment service is currently throttled.");
        }

        // Existing logic
        requestMap.getUrl();
        requestMap.getAmount();
        requestMap.getMsisdn();
        requestMap.getRecord();
        requestMap.getCurrency();
        requestMap.getReference();
        return new AckPaymentSent(UUID)
                .setStatus(1000L)
                .setMessage("OK but this is only a test")
                .setRecord(requestMap.getRecord());
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
                .setAckPaymentSent(ackPaymentSent.getId());
    }

    // Custom exception for throttling
    public static class ThrottlingException extends RuntimeException {
        public ThrottlingException(String message) {
            super(message);
        }
    }
}
