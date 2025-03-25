package com.example.poc.service;

import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class PaymentProviderMockTest {

    private PaymentProviderMock paymentProviderMock;

    @Mock
    private PaymentRecord mockRecord;

    @Mock
    private SendPaymentRequest mockRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        paymentProviderMock = new PaymentProviderMock();
    }

    @Test
    void sendPayment_ShouldReturnValidAckPaymentSent() {
        // Arrange
        when(mockRequest.getRecord()).thenReturn(mockRecord);

        // Act
        AckPaymentSent result = paymentProviderMock.sendPayment(mockRequest);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentProviderMock.UUID, result.getConversationID());
        assertEquals(1000L, result.getStatus());
        assertEquals("OK but this is only a test", result.getMessage());
        assertEquals(mockRecord, result.getRecord());
    }

    @Test
    void getPaymentStatus_ShouldReturnValidPaymentStatus() {
        // Arrange
        AckPaymentSent ackPaymentSent = new AckPaymentSent(PaymentProviderMock.UUID);

        // Act
        PaymentStatus result = paymentProviderMock.getPaymentStatus(ackPaymentSent);

        // Assert
        assertNotNull(result);
        assertEquals("101", result.getReference());
        assertEquals("nada", result.getStatus());
        assertEquals(new BigDecimal("1.01"), result.getFee());
        assertEquals("This is a test", result.getMessage());
        assertEquals(ackPaymentSent, result.getAckPaymentSent());
    }

    @Test
    void sendPayment_ShouldCallAllGetters() {
        // Act
        paymentProviderMock.sendPayment(mockRequest);

        // Assert - verify all getters are called
        verify(mockRequest).getUrl();
        verify(mockRequest).getAmount();
        verify(mockRequest).getMsisdn();
        verify(mockRequest).getReference();
        verify(mockRequest).getCurrency();
        verify(mockRequest).getReference();
    }
}