package com.example.poc.command;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.service.PaymentProviderConfig;
import com.example.poc.service.PaymentProviderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class PollAckPaymentSentCommandTest {

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private PaymentProviderConfig paymentProviderConfig;

    @InjectMocks
    private PollAckPaymentSentCommand pollAckPaymentSentCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        pollAckPaymentSentCommand = new PollAckPaymentSentCommand(executor, paymentProviderService, paymentProviderConfig);
    }

    @Test
    void testExecute() throws JsonProcessingException {
        // Given
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        PaymentStatus expectedStatus = new PaymentStatus();

        when(paymentProviderConfig.waitMilliseconds()).thenReturn(100.0);
        when(paymentProviderService.getPaymentStatus(ackPaymentSent)).thenReturn(expectedStatus);

        // When
        Uni<PaymentStatus> result = pollAckPaymentSentCommand.execute(ackPaymentSent);

        // Then
        result.subscribe().with(status -> assertEquals(expectedStatus, status));
    }

    @Test
    void testExecuteJsonProcessingException() throws JsonProcessingException {
        // Given
        AckPaymentSent ackPaymentSent = new AckPaymentSent();

        when(paymentProviderConfig.waitMilliseconds()).thenReturn(100.0);
        when(paymentProviderService.getPaymentStatus(ackPaymentSent)).thenThrow(JsonProcessingException.class);

        // When & Then
        assertThrows(RuntimeException.class, () -> pollAckPaymentSentCommand.execute(ackPaymentSent).await().indefinitely());
    }

}