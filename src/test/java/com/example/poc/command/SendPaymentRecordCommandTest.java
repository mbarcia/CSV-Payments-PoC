package com.example.poc.command;

import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentRecordRepository;
import com.example.poc.service.PaymentProviderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class SendPaymentRecordCommandTest {

    @Mock
    PaymentProviderServiceImpl paymentProviderServiceImpl;

    PaymentRecordRepository repository = mock(PaymentRecordRepository.class);

    SendPaymentRecordCommand sendPaymentRecordCommand;

    PaymentOutput paymentOutput;
    AckPaymentSent ackPaymentSent;
    PaymentRecord paymentRecord;
    PaymentStatus paymentStatus;
    SendPaymentRequest sendPaymentRequest;

    @BeforeEach
    void setUp() {
        paymentRecord = new PaymentRecord("1", "Mariano", new BigDecimal("123.50"), Currency.getInstance("GBP"));
        ackPaymentSent = (new AckPaymentSent()).setConversationID(PaymentProviderServiceImpl.UUID).setStatus(0L).setMessage("nada").setRecord(paymentRecord);
        paymentStatus = new PaymentStatus("nada").setAckPaymentSent(ackPaymentSent);
        ackPaymentSent = new AckPaymentSent(PaymentProviderServiceImpl.UUID)
                .setStatus(1000L)
                .setMessage("OK but this is only a test")
                .setRecord(paymentRecord);

        paymentOutput = new PaymentOutput(
                paymentRecord,
                paymentRecord.getCsvPaymentsOutputFile(),
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

        lenient().doReturn(ackPaymentSent).when(paymentProviderServiceImpl).sendPayment(any(SendPaymentRequest.class));
        doNothing().when(repository).persist(any(PaymentRecord.class));
        sendPaymentRecordCommand = new SendPaymentRecordCommand(paymentProviderServiceImpl);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void execute() {
        assertEquals(ackPaymentSent, sendPaymentRecordCommand.execute(paymentRecord));
    }
}