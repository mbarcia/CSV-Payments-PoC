package com.example.poc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileListingServiceImplTest {

    private FileListingServiceImpl fileListingService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileListingService = new FileListingServiceImpl();
    }

    @Test
    void listCsvFiles_WithOnlyCsvFiles_ShouldReturnAllFiles() throws IOException {
        // Arrange
        createFile("file1.csv");
        createFile("file2.CSV");
        createFile("file3.csv");

        // Act
        File[] result = fileListingService.listCsvFiles(tempDir.toString());

        // Assert
        assertNotNull(result);
        assertEquals(3, result.length);
        assertTrue(containsFileWithName(result, "file1.csv"));
        assertTrue(containsFileWithName(result, "file2.CSV"));
        assertTrue(containsFileWithName(result, "file3.csv"));
    }

    @Test
    void listCsvFiles_WithMixedFiles_ShouldReturnOnlyCsvFiles() throws IOException {
        // Arrange
        createFile("file1.csv");
        createFile("file2.txt");
        createFile("file3.csv");
        createFile("file4.pdf");
        createFile("file5.CSV");

        // Act
        File[] result = fileListingService.listCsvFiles(tempDir.toString());

        // Assert
        assertNotNull(result);
        assertEquals(3, result.length);
        assertTrue(containsFileWithName(result, "file1.csv"));
        assertTrue(containsFileWithName(result, "file3.csv"));
        assertTrue(containsFileWithName(result, "file5.CSV"));
    }

    @Test
    void listCsvFiles_WithEmptyDirectory_ShouldReturnEmptyArray() {
        // Act
        File[] result = fileListingService.listCsvFiles(tempDir.toString());

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void listCsvFiles_WithNonExistentDirectory_ShouldReturnNull() {
        // Act
        File[] result = fileListingService.listCsvFiles("/non/existent/path");

        // Assert
        assertNull(result);
    }

    @Test
    void listCsvFiles_WithCsvInFileName_ShouldOnlyMatchExtension() throws IOException {
        // Arrange
        createFile("csv.txt");
        createFile("mycsv.pdf");
        createFile("actual.csv");
        createFile("csv_file");

        // Act
        File[] result = fileListingService.listCsvFiles(tempDir.toString());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
        assertTrue(containsFileWithName(result, "actual.csv"));
    }

    private void createFile(String fileName) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        boolean created = file.createNewFile();
        assertTrue(created, "Failed to create test file: " + fileName);
    }

    private boolean containsFileWithName(File[] files, String fileName) {
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }
}
