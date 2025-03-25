package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.service.PollAckPaymentSentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessAckPaymentSentCommandTest {

    @Mock
    private PollAckPaymentSentService pollAckPaymentSentService;

    private ProcessAckPaymentSentCommand command;

    @BeforeEach
    void setUp() {
        command = new ProcessAckPaymentSentCommand(pollAckPaymentSentService);
    }

    @Test
    void execute_ShouldReturnPaymentStatus() {
        // Arrange
        AckPaymentSent ackPaymentSent = new AckPaymentSent(); // Create with necessary parameters
        PaymentStatus expectedStatus = new PaymentStatus(); // Create with expected values

        when(pollAckPaymentSentService.process(any(AckPaymentSent.class)))
                .thenReturn(expectedStatus);

        // Act
        PaymentStatus result = command.execute(ackPaymentSent);

        // Assert
        assertNotNull(result);
        assertEquals(expectedStatus, result);
        verify(pollAckPaymentSentService).process(ackPaymentSent);
    }

    @Test
    void execute_WhenServiceThrowsException_ShouldHandleError() {
        // Arrange
        AckPaymentSent ackPaymentSent = new AckPaymentSent(); // Create with necessary parameters

        when(pollAckPaymentSentService.process(any(AckPaymentSent.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> command.execute(ackPaymentSent));

        verify(pollAckPaymentSentService).process(ackPaymentSent);
    }
}
