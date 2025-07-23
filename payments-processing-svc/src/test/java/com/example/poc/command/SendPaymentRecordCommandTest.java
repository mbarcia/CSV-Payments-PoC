package com.example.poc.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.mapper.SendPaymentRequestMapper;
import com.example.poc.service.PaymentProviderServiceMock;
import io.smallrye.mutiny.Uni;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SendPaymentRecordCommandTest {

  @Mock private PaymentProviderServiceMock paymentProviderServiceMock;

  private SendPaymentRecordCommand sendPaymentRecordCommand;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    sendPaymentRecordCommand = new SendPaymentRecordCommand(paymentProviderServiceMock);
  }

  @Test
  @DisplayName("Should successfully send payment record and return AckPaymentSent")
  void execute_happyPath_shouldReturnAckPaymentSent() {
    // Given
    PaymentRecord paymentRecord =
        new PaymentRecord(
            "csvId-" + UUID.randomUUID(),
            "John Doe",
            new BigDecimal("100.00"),
            java.util.Currency.getInstance("USD"));
    paymentRecord.setId(
        UUID.randomUUID()); // Set the ID as it's generated in BaseEntity's constructor

    AckPaymentSent expectedAck = new AckPaymentSent();
    expectedAck.setPaymentRecordId(paymentRecord.getId());
    expectedAck.setStatus(1L); // Assuming 1L for ACKNOWLEDGED status
    expectedAck.setMessage("Payment sent successfully");

    when(paymentProviderServiceMock.sendPayment(
            any(SendPaymentRequestMapper.SendPaymentRequest.class)))
        .thenReturn(expectedAck);

    // When
    Uni<AckPaymentSent> resultUni = sendPaymentRecordCommand.execute(paymentRecord);

    // Then
    AckPaymentSent actualAck = resultUni.await().indefinitely();

    assertThat(actualAck).isEqualTo(expectedAck);

    ArgumentCaptor<SendPaymentRequestMapper.SendPaymentRequest> requestCaptor =
        ArgumentCaptor.forClass(SendPaymentRequestMapper.SendPaymentRequest.class);
    verify(paymentProviderServiceMock, times(1)).sendPayment(requestCaptor.capture());

    SendPaymentRequestMapper.SendPaymentRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.getAmount()).isEqualTo(paymentRecord.getAmount());
    assertThat(capturedRequest.getCurrency()).isEqualTo(paymentRecord.getCurrency());
    assertThat(capturedRequest.getReference()).isEqualTo(paymentRecord.getRecipient());
    assertThat(capturedRequest.getPaymentRecordId()).isEqualTo(paymentRecord.getId());
    assertThat(capturedRequest.getPaymentRecord()).isEqualTo(paymentRecord);
  }

  @Test
  @DisplayName("Should propagate exception when paymentProviderServiceMock throws an exception")
  void execute_unhappyPath_shouldPropagateException() {
    // Given
    PaymentRecord paymentRecord =
        new PaymentRecord(
            "csvId-" + UUID.randomUUID(),
            "John Doe",
            new BigDecimal("100.00"),
            java.util.Currency.getInstance("USD"));
    paymentRecord.setId(
        UUID.randomUUID()); // Set the ID as it's generated in BaseEntity's constructor

    RuntimeException expectedException = new RuntimeException("Payment processing failed");
    when(paymentProviderServiceMock.sendPayment(
            any(SendPaymentRequestMapper.SendPaymentRequest.class)))
        .thenThrow(expectedException);

    // Then
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> sendPaymentRecordCommand.execute(paymentRecord).await().indefinitely());
    assertThat(thrown).isEqualTo(expectedException);

    verify(paymentProviderServiceMock, times(1))
        .sendPayment(any(SendPaymentRequestMapper.SendPaymentRequest.class));
  }
}
