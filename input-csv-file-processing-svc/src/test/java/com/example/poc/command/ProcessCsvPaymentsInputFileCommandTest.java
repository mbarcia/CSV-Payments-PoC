package com.example.poc.command;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessCsvPaymentsInputFileCommandTest {

    @TempDir
    Path tempDir;

    private Path tempCsvFile;

    @InjectMocks
    private ProcessCsvPaymentsInputFileCommand command;

    @BeforeEach
    void setUp() throws IOException {
        tempCsvFile = tempDir.resolve("test.csv");
        String csvContent = "ID,Recipient,Amount,Currency\n"
                + UUID.randomUUID() + ",John Doe,100.00,USD\n"
                + UUID.randomUUID() + ",Jane Smith,200.50,EUR\n";
        Files.writeString(tempCsvFile, csvContent);
        MockitoAnnotations.openMocks(this);
        command.setExecutor(Runnable::run);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempCsvFile);
    }

    @Test
    void execute() {
        // Given
        CsvPaymentsInputFile csvFile = new CsvPaymentsInputFile(tempCsvFile.toFile());

        // When
        Multi<PaymentRecord> resultMulti = command.execute(csvFile);

        // Then
        AssertSubscriber<PaymentRecord> subscriber = resultMulti.subscribe().withSubscriber(AssertSubscriber.create(2));
        subscriber.awaitCompletion();

        List<PaymentRecord> records = subscriber.getItems();
        assertEquals(2, records.size());

        PaymentRecord record1 = records.getFirst();
        assertNotNull(record1.getCsvId());
        assertEquals("John Doe", record1.getRecipient());
        assertEquals(new BigDecimal("100.00"), record1.getAmount());
        assertEquals(Currency.getInstance("USD"), record1.getCurrency());
        assertEquals(csvFile.getFilepath(), record1.getCsvPaymentsInputFilePath());

        PaymentRecord record2 = records.get(1);
        assertNotNull(record2.getCsvId());
        assertEquals("Jane Smith", record2.getRecipient());
        assertEquals(new BigDecimal("200.50"), record2.getAmount());
        assertEquals(Currency.getInstance("EUR"), record2.getCurrency());
        assertEquals(csvFile.getFilepath(), record2.getCsvPaymentsInputFilePath());
    }

    @Test
    void execute_fileNotFound() {
        // Given
        CsvPaymentsInputFile csvFile = new CsvPaymentsInputFile(tempDir.resolve("nonexistent.csv").toFile());

        // When Then
        assertThrows(RuntimeException.class, () -> command.execute(csvFile));
    }

    @Test
    void execute_invalidCsvContent() throws IOException {
        // Given
        String invalidCsvContent = "ID,Recipient,Amount,Currency\n"
                + UUID.randomUUID() + ",John Doe,invalid_amount,USD\n";
        Files.writeString(tempCsvFile, invalidCsvContent);
        CsvPaymentsInputFile csvFile = new CsvPaymentsInputFile(tempCsvFile.toFile());

        // When
        Multi<PaymentRecord> resultMulti = command.execute(csvFile);

        // Then
        AssertSubscriber<PaymentRecord> subscriber = resultMulti.subscribe().withSubscriber(AssertSubscriber.create(1));
        subscriber.awaitFailure();
        subscriber.assertFailedWith(RuntimeException.class);
    }
}
