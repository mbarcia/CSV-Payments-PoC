package com.example.poc.command;

import com.example.poc.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPaymentStatusCommandTest {

    private ProcessPaymentStatusCommand command;
    private PaymentStatus paymentStatus;
    private PaymentRecord paymentRecord;
    private CsvPaymentsOutputFile csvOutFile;

    @BeforeEach
    void setUp() throws IOException {
        command = new ProcessPaymentStatusCommand();

        // Setup PaymentRecord
        paymentRecord = new PaymentRecord();
        csvOutFile = new CsvPaymentsOutputFile(new CsvPaymentsInputFile(new File("test-output.csv")));
        paymentRecord.setCsvPaymentsOutputFile(csvOutFile);
        paymentRecord.setCsvId("CSV123");
        paymentRecord.setRecipient("John Doe");
        paymentRecord.setAmount(new BigDecimal("100.00"));
        paymentRecord.setCurrency(Currency.getInstance("USD"));

        // Setup AckPaymentSent
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        ackPaymentSent.setRecord(paymentRecord);
        ackPaymentSent.setConversationID("CONV123");
        ackPaymentSent.setStatus(100L);

        // Setup PaymentStatus
        paymentStatus = new PaymentStatus();
        paymentStatus.setAckPaymentSent(ackPaymentSent);
        paymentStatus.setMessage("Payment processed successfully");
        paymentStatus.setFee(new BigDecimal("2.50"));
    }

    @Test
    void execute_ShouldCreatePaymentOutputWithCorrectValues() {
        // When
        PaymentOutput result = command.execute(paymentStatus);

        // Then
        assertNotNull(result);
        assertSame(paymentRecord, result.getPaymentRecord());
        assertEquals(csvOutFile, result.getCsvPaymentsOutputFile());
        assertEquals("CSV123", result.getCsvId());
        assertEquals("John Doe", result.getRecipient());
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(Currency.getInstance("USD"), result.getCurrency());
        assertEquals("CONV123", result.getConversationID());
        assertEquals(100L, result.getStatus());
        assertEquals("Payment processed successfully", result.getMessage());
        assertEquals(new BigDecimal("2.50"), result.getFee());
    }

    @Test
    void execute_WithNullValues_ShouldNotThrowException() {
        // Given
        PaymentStatus emptyStatus = new PaymentStatus();
        AckPaymentSent emptyAck = new AckPaymentSent();
        PaymentRecord emptyRecord = new PaymentRecord();
        emptyAck.setRecord(emptyRecord);
        emptyStatus.setAckPaymentSent(emptyAck);

        // When
        PaymentOutput result = command.execute(emptyStatus);

        // Then
        assertNotNull(result);
        assertSame(emptyRecord, result.getPaymentRecord());
        assertNull(result.getCsvPaymentsOutputFile());
        assertNull(result.getCsvId());
        assertNull(result.getRecipient());
        assertNull(result.getAmount());
        assertNull(result.getCurrency());
        assertNull(result.getConversationID());
        assertNull(result.getStatus());
        assertNull(result.getMessage());
        assertNull(result.getFee());
    }
}