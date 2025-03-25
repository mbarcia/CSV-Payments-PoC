package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.PaymentRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProcessCsvPaymentsInputFileCommandTest {

    private ProcessCsvPaymentsInputFileCommand command;
    private CsvPaymentsInputFile csvFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        command = new ProcessCsvPaymentsInputFileCommand();
        csvFile = new CsvPaymentsInputFile();
    }

    @Test
    void execute_WithValidCsvFile_ShouldReturnPaymentRecordStream() throws IOException {
        // Arrange
        File testFile = createTestCsvFile();
        csvFile.setFilepath(testFile.getAbsolutePath());

        // Act
        Stream<PaymentRecord> result = command.execute(csvFile);

        // Assert
        List<PaymentRecord> records = result.toList();
        assertNotNull(records);
        assertEquals(2, records.size());

        // Verify first record
        PaymentRecord firstRecord = records.getFirst();
        assertEquals("1", firstRecord.getCsvId());
        assertEquals(csvFile, firstRecord.getCsvPaymentsInputFile());

        // Verify second record
        PaymentRecord secondRecord = records.get(1);
        assertEquals("2", secondRecord.getCsvId());
        assertEquals(csvFile, secondRecord.getCsvPaymentsInputFile());
    }

    @Test
    void execute_WithInvalidFilePath_ShouldThrowRuntimeException() {
        // Arrange
        csvFile.setFilepath("nonexistent/file.csv");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> command.execute(csvFile));
    }

    @Test
    void execute_WithEmptyFile_ShouldReturnEmptyStream() throws IOException {
        // Arrange
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        try (FileWriter writer = new FileWriter(emptyFile)) {
            writer.write("ID,Recipient,Amount,Currency\n"); // just headers
        }
        csvFile.setFilepath(emptyFile.getAbsolutePath());

        // Act
        Stream<PaymentRecord> result = command.execute(csvFile);

        // Assert
        List<PaymentRecord> records = result.toList();
        assertTrue(records.isEmpty());
    }

    private File createTestCsvFile() throws IOException {
        File testFile = tempDir.resolve("test.csv").toFile();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("ID,Recipient,Amount,Currency\n"); // headers
            writer.write("1,Juan,101.01,USD\n");           // first record
            writer.write("2,Pedro,202.02,EUR\n");           // second record
        }
        return testFile;
    }
}
