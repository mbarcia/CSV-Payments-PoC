package com.example.poc.service;

import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentProviderMockTest {

    @InjectMocks
    PaymentProviderMock paymentProviderMock;
    AckPaymentSent ackPaymentSent;
    SendPaymentRequest paymentRequest;
    PaymentRecord paymentRecord;
    String uuid;

    @BeforeEach
    void setUp() {
        uuid = String.valueOf(UUID.randomUUID());
        paymentProviderMock = new PaymentProviderMock();
        ackPaymentSent = new AckPaymentSent(PaymentProviderMock.UUID);
        paymentRecord = new PaymentRecord("1", "Mariano", new BigDecimal("123.50"), Currency.getInstance("GBP"));
        paymentRequest = new SendPaymentRequest()
                .setAmount(paymentRecord.getAmount())
                .setReference("")
                .setCurrency(paymentRecord.getCurrency())
                .setRecord(paymentRecord);
    }

    @Test
    void sendPayment() {
        assertEquals(paymentProviderMock.sendPayment(paymentRequest), new AckPaymentSent(PaymentProviderMock.UUID)
                .setStatus(1000L)
                .setMessage("OK but this is only a test")
                .setRecord(paymentRecord));
    }

    @Test
    void getPaymentStatus() {
        assertEquals(paymentProviderMock.getPaymentStatus(ackPaymentSent), new PaymentStatus("101")
                .setStatus("nada")
                .setFee(new BigDecimal("1.01"))
                .setMessage("This is a test")
                .setAckPaymentSent(ackPaymentSent));
    }
}