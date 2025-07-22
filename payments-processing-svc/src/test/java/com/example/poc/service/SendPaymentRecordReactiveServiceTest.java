package com.example.poc.service;

import com.example.poc.command.SendPaymentRecordCommand;
import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentRecord;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class SendPaymentRecordReactiveServiceTest {

    @Mock
    private SendPaymentRecordCommand sendPaymentRecordCommand;

    @Mock
    private PaymentRecord paymentRecord;

    @InjectMocks
    private SendPaymentRecordReactiveService sendPaymentRecordReactiveService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExecute() {
        // Given
        AckPaymentSent expectedAck = new AckPaymentSent();

        when(sendPaymentRecordCommand.execute(paymentRecord)).thenReturn(Uni.createFrom().item(expectedAck));
        doReturn(Uni.createFrom().item(paymentRecord)).when(paymentRecord).save();

        // When
        Uni<AckPaymentSent> result = sendPaymentRecordReactiveService.process(paymentRecord);

        // Then
        result.subscribe().with(ack -> assertEquals(expectedAck, ack));
    }
}