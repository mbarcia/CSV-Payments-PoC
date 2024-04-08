package com.example.poc.command;

import com.example.poc.client.PaymentProviderMock;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.repository.PaymentStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class UnparseRecordCommandTest {

    UnparseRecordCommand unparseRecordCommand;

    PaymentStatusRepository repository = mock(PaymentStatusRepository.class);

    PaymentOutput paymentOutput;
    AckPaymentSent ackPaymentSent;
    PaymentRecord paymentRecord;
    PaymentStatus paymentStatus;

    @BeforeEach
    void setUp() {
        paymentRecord = new PaymentRecord("1", "Mariano", new BigDecimal("123.50"), Currency.getInstance("GBP"));
        ackPaymentSent = (new AckPaymentSent()).setConversationID(PaymentProviderMock.UUID).setStatus(0L).setMessage("nada").setRecord(paymentRecord);
        paymentStatus = new PaymentStatus("nada").setAckPaymentSent(ackPaymentSent);
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

        when(repository.save(any(PaymentStatus.class))).thenReturn(null);
        unparseRecordCommand = new UnparseRecordCommand(repository);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void execute() {
        assertEquals(paymentOutput, unparseRecordCommand.execute(paymentStatus));
    }
}