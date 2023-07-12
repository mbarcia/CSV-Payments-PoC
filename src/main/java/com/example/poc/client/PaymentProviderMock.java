package com.example.poc.client;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component(value="mock")
public class PaymentProviderMock implements PaymentProvider {
    @Override
    public AckPaymentSent sendPayment(SendPaymentRequest requestMap) {

        return new AckPaymentSent(String.valueOf(UUID.randomUUID()))
                .setStatus(1000L)
                .setMessage("OK but this is only a test")
                .setRecord(requestMap.getRecord());
    }

    @Override
    public PaymentStatus getPaymentStatus(AckPaymentSent ackPaymentSent) throws JsonProcessingException {

        return new PaymentStatus("101")
                .setStatus("nada")
                .setFee(new BigDecimal("1.01"))
                .setMessage("This is a test")
                .setAckPaymentSent(ackPaymentSent);
    }
}
