package com.example.poc.service;

import com.example.poc.command.ProcessPaymentStatusCommand;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.domain.PaymentStatus;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessPaymentStatusReactiveServiceTest {

    @InjectMocks
    ProcessPaymentStatusReactiveService service;

    @Mock
    ProcessPaymentStatusCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @RunOnVertxContext
    void process() {
        // Given
        PaymentRecord paymentRecord = new com.example.poc.common.domain.PaymentRecord(
            UUID.randomUUID().toString(),
            "recipient123",
            new java.math.BigDecimal("100.00"),
            java.util.Currency.getInstance("USD")
        );
        com.example.poc.common.domain.AckPaymentSent ackPaymentSent = new com.example.poc.common.domain.AckPaymentSent(
            UUID.randomUUID())
            .setPaymentRecord(paymentRecord)
            .setStatus(1L)
            .setMessage("SUCCESS");
        PaymentStatus paymentStatus = mock(PaymentStatus.class);
        when(paymentStatus.getReference()).thenReturn(UUID.randomUUID().toString());
        when(paymentStatus.getAckPaymentSent()).thenReturn(ackPaymentSent);
        when(paymentStatus.getMessage()).thenReturn("Payment processed successfully");
        when(paymentStatus.getFee()).thenReturn(new java.math.BigDecimal("1.50"));
        when(paymentStatus.save()).thenReturn(Uni.createFrom().voidItem());

        PaymentOutput expectedOutput = new PaymentOutput(
            new PaymentStatus(),
            paymentRecord.getCsvId(),
            paymentRecord.getRecipient(),
            paymentRecord.getAmount(),
            paymentRecord.getCurrency(),
            ackPaymentSent.getConversationId(),
            ackPaymentSent.getStatus(),
            ackPaymentSent.getMessage(),
            new BigDecimal("1.50")
        );

        when(command.execute(any(PaymentStatus.class))).thenReturn(Uni.createFrom().item(expectedOutput));

        // When
        Uni<PaymentOutput> resultUni = service.process(paymentStatus);

        // Then
        UniAssertSubscriber<PaymentOutput> subscriber = resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem();
        assertNotNull(subscriber.getItem());
    }
}