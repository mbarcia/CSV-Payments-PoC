package com.example.poc.client;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface PaymentProvider {
    AckPaymentSent sendPayment(SendPaymentRequest requestMap);
    PaymentStatus getPaymentStatus(AckPaymentSent ackPaymentSent) throws JsonProcessingException;
}
