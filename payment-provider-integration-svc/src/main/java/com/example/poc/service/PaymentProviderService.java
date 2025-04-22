package com.example.poc.service;

import com.example.poc.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface PaymentProviderService {
    AckPaymentSent sendPayment(SendPaymentRequest requestMap);
    PaymentStatus getPaymentStatus(AckPaymentSent ackPaymentSent) throws JsonProcessingException;
}
