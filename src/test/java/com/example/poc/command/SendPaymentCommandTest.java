package com.example.poc.command;

import com.example.poc.client.PaymentProviderMock;
import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentRecordRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SendPaymentCommandTest {

    @Mock
    PaymentProviderMock paymentProviderMock;

    PaymentRecordRepository repository = mock(PaymentRecordRepository.class);

    SendPaymentCommand sendPaymentCommand;

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
                .setUrl("")
                .setMsisdn("")
                .setReference(paymentRecord.getRecipient())
                .setCurrency(paymentRecord.getCurrency())
                .setRecord(paymentRecord);

        lenient().doReturn(ackPaymentSent).when(paymentProviderMock).sendPayment(any(SendPaymentRequest.class));
        when(repository.save(any(PaymentRecord.class))).thenReturn(null);
        sendPaymentCommand = new SendPaymentCommand(paymentProviderMock, repository);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void execute() {
        assertEquals(ackPaymentSent, sendPaymentCommand.execute(paymentRecord));
    }
}