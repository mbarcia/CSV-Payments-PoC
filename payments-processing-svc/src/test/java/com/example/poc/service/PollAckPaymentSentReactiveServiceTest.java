package com.example.poc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.example.poc.command.PollAckPaymentSentCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PollAckPaymentSentReactiveServiceTest {

  @Mock private PollAckPaymentSentCommand pollAckPaymentSentCommand;

  @InjectMocks private PollAckPaymentSentReactiveService pollAckPaymentSentReactiveService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testExecute() {
    // Given
    AckPaymentSent ackPaymentSent = new AckPaymentSent();
    PaymentStatus expectedStatus = new PaymentStatus();

    when(pollAckPaymentSentCommand.execute(ackPaymentSent))
        .thenReturn(Uni.createFrom().item(expectedStatus));

    // When
    Uni<PaymentStatus> result = pollAckPaymentSentReactiveService.process(ackPaymentSent);

    // Then
    result.subscribe().with(status -> assertEquals(expectedStatus, status));
  }
}
