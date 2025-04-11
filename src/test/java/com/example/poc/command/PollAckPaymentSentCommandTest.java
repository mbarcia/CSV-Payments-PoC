package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.service.PaymentProviderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PollAckPaymentSentCommandTest {

    @Mock
    private PaymentProviderService paymentProviderServiceMock;

    private PollAckPaymentSentCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new PollAckPaymentSentCommand(paymentProviderServiceMock);
    }

    @Test
    void execute_ShouldReturnPaymentStatus() throws JsonProcessingException {
        // Arrange
        AckPaymentSent ackPaymentSent = new AckPaymentSent(); // Add necessary parameters
        PaymentStatus expectedStatus = new PaymentStatus(); // Add expected values
        when(paymentProviderServiceMock.getPaymentStatus(any(AckPaymentSent.class)))
                .thenReturn(expectedStatus);

        // Act
        PaymentStatus result = command.execute(ackPaymentSent);

        // Assert
        assertNotNull(result);
        verify(paymentProviderServiceMock).getPaymentStatus(ackPaymentSent);
        assertEquals(expectedStatus, result);
    }

    @Test
    void execute_WhenExceptionOccurs_ShouldThrowRuntimeException() throws JsonProcessingException {
        // Arrange
        AckPaymentSent ackPaymentSent = new AckPaymentSent(); // Add necessary parameters
        when(paymentProviderServiceMock.getPaymentStatus(any(AckPaymentSent.class)))
                .thenThrow(new JsonProcessingException("Error processing JSON") {});

        // Act & Assert
        assertThrows(RuntimeException.class, () -> command.execute(ackPaymentSent));
    }
}
