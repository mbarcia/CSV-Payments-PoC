package com.example.poc.service;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.mapper.SendPaymentRequestMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface PaymentProviderService {
  AckPaymentSent sendPayment(SendPaymentRequestMapper.SendPaymentRequest requestMap);

  PaymentStatus getPaymentStatus(AckPaymentSent ackPaymentSent) throws JsonProcessingException;
}
