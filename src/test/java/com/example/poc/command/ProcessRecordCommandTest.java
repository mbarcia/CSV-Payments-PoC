package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.service.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ProcessRecordCommandTest {
    @Mock
    SendPaymentService sendPaymentService;

    @Mock
    PollPaymentStatusService pollPaymentStatusService;

    @Mock
    UnparseRecordService unparseRecordService;

    @Mock
    ProcessRecordService processRecordService;

    @InjectMocks
    ProcessRecordCommand processRecordCommand;

    PaymentRecord paymentRecord;
    AckPaymentSent ackPaymentSent;
    PaymentStatus paymentStatus;
    PaymentOutput paymentOutput;

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

        doReturn(ackPaymentSent).when(sendPaymentService).process(paymentRecord);
        doReturn(paymentStatus).when(pollPaymentStatusService).process(ackPaymentSent);
        doReturn(paymentOutput).when(unparseRecordService).process(paymentStatus);
    }

    @Test
    void executeTest() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        doNothing().when(processRecordService).writeOutputToFile(paymentRecord, paymentOutput);
        assertEquals(paymentOutput, processRecordCommand.execute(paymentRecord));
    }

    @Test
    void executeWithExceptionTest() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        lenient().doThrow(CsvDataTypeMismatchException.class).when(processRecordService).writeOutputToFile(paymentRecord, paymentOutput);
        assertThrows(RuntimeException.class, () -> processRecordCommand.execute(paymentRecord));
    }

    @Test
    void executeWithAnotherExceptionTest() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        lenient().doThrow(CsvRequiredFieldEmptyException.class).when(processRecordService).writeOutputToFile(paymentRecord, paymentOutput);
        assertThrows(RuntimeException.class, () -> processRecordCommand.execute(paymentRecord));
    }
}
