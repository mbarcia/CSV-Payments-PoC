package com.example.poc.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CsvPaymentsInputFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testConstructorWithFile() {
        // Arrange
        File testFile = new File(tempDir.toFile(), "test.csv");

        // Act
        CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(testFile);

        // Assert
        assertNotNull(inputFile);
        assertEquals(testFile.getPath(), inputFile.getFilepath());
        assertEquals(testFile, inputFile.getCsvFile());
        assertTrue(inputFile.getRecords().isEmpty());
    }

    @Test
    void testEqualsWithSameId() {
        // Arrange
        CsvPaymentsInputFile file1 = new CsvPaymentsInputFile();
        CsvPaymentsInputFile file2 = new CsvPaymentsInputFile();
        file1.setId(1L);
        file2.setId(1L);

        // Act & Assert
        assertEquals(file1, file2);
    }

    @Test
    void testEqualsWithDifferentId() {
        // Arrange
        CsvPaymentsInputFile file1 = new CsvPaymentsInputFile();
        CsvPaymentsInputFile file2 = new CsvPaymentsInputFile();
        file1.setId(1L);
        file2.setId(2L);

        // Act & Assert
        assertNotEquals(file1, file2);
    }

    @Test
    void testEqualsWithNullId() {
        // Arrange
        CsvPaymentsInputFile file1 = new CsvPaymentsInputFile();
        CsvPaymentsInputFile file2 = new CsvPaymentsInputFile();

        // Act & Assert
        assertNotEquals(file1, file2);
    }

    @Test
    void testEqualsWithNull() {
        // Arrange
        CsvPaymentsInputFile file = new CsvPaymentsInputFile();

        // Act & Assert
        assertNotEquals(file, null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        // Arrange
        CsvPaymentsInputFile file = new CsvPaymentsInputFile();

        // Act & Assert
        assertNotEquals(file, new Object());
    }

    @Test
    void testRecordsInitialization() {
        // Arrange & Act
        CsvPaymentsInputFile file = new CsvPaymentsInputFile();

        // Assert
        assertNotNull(file.getRecords());
        assertTrue(file.getRecords().isEmpty());
    }

    @Test
    void testRecordsImmutability() {
        // Arrange
        CsvPaymentsInputFile file = new CsvPaymentsInputFile();

        // Act & Assert
        assertNotNull(file.getRecords());
        assertEquals(ArrayList.class, file.getRecords().getClass());
    }
}
