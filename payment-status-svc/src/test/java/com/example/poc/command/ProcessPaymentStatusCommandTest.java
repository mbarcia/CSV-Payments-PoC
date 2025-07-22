package com.example.poc.command;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.domain.PaymentStatus;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProcessPaymentStatusCommandTest {

    @InjectMocks
    ProcessPaymentStatusCommand processPaymentStatusCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute() {
        // Given
        PaymentRecord paymentRecord = new PaymentRecord(
                UUID.randomUUID().toString(),
                "recipient123",
                new BigDecimal("100.00"),
                java.util.Currency.getInstance("USD")
        );
        AckPaymentSent ackPaymentSent = new AckPaymentSent().setPaymentRecord(paymentRecord).setStatus(1L).setMessage("SUCCESS");
        PaymentStatus paymentStatus = new PaymentStatus().setAckPaymentSent(ackPaymentSent).setMessage("Payment processed successfully").setFee(new BigDecimal("1.50"));

        // When
        Uni<PaymentOutput> resultUni = processPaymentStatusCommand.execute(paymentStatus);

        // Then
        UniAssertSubscriber<PaymentOutput> subscriber = resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        PaymentOutput paymentOutput = subscriber.awaitItem().getItem();

        assertNotNull(paymentOutput);
        assertEquals(paymentStatus, paymentOutput.getPaymentStatus());
        assertEquals(paymentRecord.getCsvId(), paymentOutput.getCsvId());
        assertEquals(paymentRecord.getRecipient(), paymentOutput.getRecipient());
        assertEquals(paymentRecord.getAmount(), paymentOutput.getAmount());
        assertEquals(paymentRecord.getCurrency(), paymentOutput.getCurrency());
        assertEquals(ackPaymentSent.getConversationId(), paymentOutput.getConversationId());
        assertEquals(ackPaymentSent.getStatus(), paymentOutput.getStatus());
        assertEquals(paymentStatus.getMessage(), paymentOutput.getMessage());
        assertEquals(paymentStatus.getFee(), paymentOutput.getFee());
    }
}