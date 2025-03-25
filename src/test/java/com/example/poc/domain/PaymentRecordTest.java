package com.example.poc.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRecordTest {

    @Test
    void testPaymentRecordCreation() {
        // Given
        String csvId = "PAY123";
        String recipient = "John Doe";
        BigDecimal amount = new BigDecimal("1000.00");
        Currency currency = Currency.getInstance("GBP");

        // When
        PaymentRecord paymentRecord = new PaymentRecord(csvId, recipient, amount, currency);

        // Then
        assertNotNull(paymentRecord);
        assertEquals(csvId, paymentRecord.getCsvId());
        assertEquals(recipient, paymentRecord.getRecipient());
        assertEquals(amount, paymentRecord.getAmount());
        assertEquals(currency, paymentRecord.getCurrency());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        PaymentRecord record1 = new PaymentRecord("PAY123", "John Doe", new BigDecimal("1000.00"), Currency.getInstance("GBP"));
        PaymentRecord record2 = new PaymentRecord("PAY123", "John Doe", new BigDecimal("1000.00"), Currency.getInstance("GBP"));
        PaymentRecord record3 = new PaymentRecord("PAY124", "Jane Doe", new BigDecimal("2000.00"), Currency.getInstance("USD"));

        // Then
        assertTrue(EqualsBuilder.reflectionEquals(record1, record2));
        assertNotEquals(record1, record3);
        assertEquals(record1.hashCode(), record2.hashCode());
        assertNotEquals(record1.hashCode(), record3.hashCode());
    }

    @Test
    void testToString() {
        // Given
        PaymentRecord record = new PaymentRecord("PAY123", "John Doe", new BigDecimal("1000.00"), Currency.getInstance("GBP"));

        // When
        String result = record.toString();

        // Then
        assertTrue(result.contains("PAY123"));
        assertTrue(result.contains("John Doe"));
        assertTrue(result.contains("1.000")); // English-UK formatted
        assertTrue(result.contains("GBP"));
    }

    @Test
    void testChainedSetters() {
        // Given
        PaymentRecord record = new PaymentRecord();

        // When
        record.setCsvId("PAY123")
                .setRecipient("John Doe")
                .setAmount(new BigDecimal("1000.00"))
                .setCurrency(Currency.getInstance("GBP"));

        // Then
        assertEquals("PAY123", record.getCsvId());
        assertEquals("John Doe", record.getRecipient());
        assertEquals(new BigDecimal("1000.00"), record.getAmount());
        assertEquals(Currency.getInstance("GBP"), record.getCurrency());
    }

    @Test
    void testRelationships() {
        // Given
        PaymentRecord record = new PaymentRecord("PAY123", "John Doe", new BigDecimal("1000.00"), Currency.getInstance("GBP"));
        CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile();
        CsvPaymentsOutputFile outputFile = new CsvPaymentsOutputFile();
        AckPaymentSent ackPaymentSent = new AckPaymentSent();

        // When
        record.setCsvPaymentsInputFile(inputFile);
        record.setCsvPaymentsOutputFile(outputFile);
        record.setAckPaymentSent(ackPaymentSent);

        // Then
        assertEquals(inputFile, record.getCsvPaymentsInputFile());
        assertEquals(outputFile, record.getCsvPaymentsOutputFile());
        assertEquals(ackPaymentSent, record.getAckPaymentSent());
    }
}
