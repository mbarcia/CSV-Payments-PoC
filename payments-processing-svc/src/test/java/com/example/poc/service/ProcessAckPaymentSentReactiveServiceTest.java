package com.example.poc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.example.poc.command.ProcessAckPaymentSentCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessAckPaymentSentReactiveServiceTest {

  @Mock private ProcessAckPaymentSentCommand processAckPaymentSentCommand;

  @Mock private AckPaymentSent ackPaymentSent;

  @InjectMocks private ProcessAckPaymentSentReactiveService processAckPaymentSentReactiveService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testExecute() {
    // Given
    PaymentStatus expectedStatus = new PaymentStatus();

    when(processAckPaymentSentCommand.execute(ackPaymentSent))
        .thenReturn(Uni.createFrom().item(expectedStatus));
    doReturn(Uni.createFrom().item(ackPaymentSent)).when(ackPaymentSent).save();

    // When
    Uni<PaymentStatus> result = processAckPaymentSentReactiveService.process(ackPaymentSent);

    // Then
    result.subscribe().with(status -> assertEquals(expectedStatus, status));
  }
}
