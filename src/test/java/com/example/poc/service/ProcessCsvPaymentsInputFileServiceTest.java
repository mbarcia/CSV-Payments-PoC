package com.example.poc.service;

import com.example.poc.command.ProcessCsvPaymentsInputFileCommand;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.repository.CsvPaymentsInputFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProcessCsvPaymentsInputFileServiceTest {

    @Mock
    private CsvPaymentsInputFileRepository repository;

    @Mock
    private ProcessCsvPaymentsInputFileCommand command;

    private ProcessCsvPaymentsInputFileService service;

    @BeforeEach
    void setUp() {
        service = new ProcessCsvPaymentsInputFileService(repository, command);
    }

    @Test
    void createCsvFile_WithValidFile_ShouldReturnCsvPaymentsInputFile(@TempDir Path tempDir) throws IOException {
        // Arrange
        File testFile = tempDir.resolve("test.csv").toFile();
        assertTrue(testFile.createNewFile());

        // Act
        CsvPaymentsInputFile result = service.createCsvFile(testFile);

        // Assert
        assertNotNull(result);
        assertEquals(testFile, result.getCsvFile());
    }

    @Test
    void createCsvFile_WithNullFile_ShouldThrowException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> service.createCsvFile(null));
    }

    @Test
    void constructor_ShouldInitializeWithDependencies() {
        // Assert
        assertNotNull(service);
    }

    @Test
    void createCsvFile_WithNonExistentFile_ShouldReturnCsvPaymentsInputFile() {
        // Arrange
        File nonExistentFile = new File("nonexistent.csv");

        // Act
        CsvPaymentsInputFile result = service.createCsvFile(nonExistentFile);

        // Assert
        assertNotNull(result);
        assertEquals(nonExistentFile, result.getCsvFile());
    }
}
