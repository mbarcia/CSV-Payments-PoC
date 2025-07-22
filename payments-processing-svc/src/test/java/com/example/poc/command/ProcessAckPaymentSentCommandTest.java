package com.example.poc.command;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.service.PollAckPaymentSentReactiveService;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class ProcessAckPaymentSentCommandTest {

    @Mock
    private PollAckPaymentSentReactiveService pollAckPaymentSentReactiveService;

    @InjectMocks
    private ProcessAckPaymentSentCommand processAckPaymentSentCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExecute() {
        // Given
        UUID uuid = UUID.randomUUID();
        AckPaymentSent ackPaymentSent = new AckPaymentSent().setConversationId(uuid);
        PaymentStatus expectedStatus = new PaymentStatus();

        when(pollAckPaymentSentReactiveService.process(ackPaymentSent)).thenReturn(Uni.createFrom().item(expectedStatus));

        // When
        Uni<PaymentStatus> result = processAckPaymentSentCommand.execute(ackPaymentSent);

        // Then
        result.subscribe().with(status -> assertEquals(expectedStatus, status));
    }
}