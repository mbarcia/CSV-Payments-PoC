package com.example.poc.command;

import com.example.poc.client.PaymentProviderMock;
import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.AckPaymentSentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PollPaymentStatusCommandTest {

    @Mock
    PaymentProviderMock paymentProviderMock;

    AckPaymentSentRepository repository = mock(AckPaymentSentRepository.class);

    PollPaymentStatusCommand pollPaymentStatusCommand;

    PaymentOutput paymentOutput;
    AckPaymentSent ackPaymentSent;
    PaymentRecord paymentRecord;
    PaymentStatus paymentStatus;
    SendPaymentRequest sendPaymentRequest;

    @BeforeEach
    void setUp() {
        paymentRecord = new PaymentRecord("1", "Mariano", new BigDecimal("123.50"), Currency.getInstance("GBP"));
        ackPaymentSent = (new AckPaymentSent()).setConversationID(PaymentProviderMock.UUID).setStatus(0L).setMessage("nada").setRecord(paymentRecord);
        paymentStatus = new PaymentStatus("nada").setAckPaymentSent(ackPaymentSent);
        ackPaymentSent = new AckPaymentSent(PaymentProviderMock.UUID)
                .setStatus(1000L)
                .setMessage("OK but this is only a test")
                .setRecord(paymentRecord);

        paymentOutput = new PaymentOutput(
                paymentRecord.getCsvId(),
                paymentRecord.getRecipient(),
                paymentRecord.getAmount(),
                paymentRecord.getCurrency(),
                paymentStatus.getAckPaymentSent().getConversationID(),
                paymentStatus.getAckPaymentSent().getStatus(),
                paymentStatus.getMessage(),
                paymentStatus.getFee()
        );
        sendPaymentRequest = new SendPaymentRequest()
                .setAmount(paymentRecord.getAmount())
                .setReference(paymentRecord.getRecipient())
                .setCurrency(paymentRecord.getCurrency())
                .setRecord(paymentRecord);

        lenient().doReturn(paymentStatus).when(paymentProviderMock).getPaymentStatus(any(AckPaymentSent.class));
        when(repository.save(any(AckPaymentSent.class))).thenReturn(null);
        pollPaymentStatusCommand = new PollPaymentStatusCommand(paymentProviderMock, repository);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void execute() {
        assertEquals(paymentStatus, pollPaymentStatusCommand.execute(ackPaymentSent));
    }

    @Test
    void executeWithJsonProcessingException() {
        doThrow(JsonProcessingException.class).when(paymentProviderMock).getPaymentStatus(any(AckPaymentSent.class));
        assertThrowsExactly(RuntimeException.class, () -> pollPaymentStatusCommand.execute(ackPaymentSent));
    }
}