package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.mapper.SendPaymentRequestMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface PaymentProviderService {
    AckPaymentSent sendPayment(SendPaymentRequestMapper.SendPaymentRequest requestMap);
    PaymentStatus getPaymentStatus(AckPaymentSent ackPaymentSent) throws JsonProcessingException;
}
