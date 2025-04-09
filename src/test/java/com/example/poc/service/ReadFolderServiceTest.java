package com.example.poc.service;

import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.repository.CsvFolderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadFolderServiceTest {

    @Mock
    private CsvFolderRepository repository;

    @Mock
    private ReadFolderCommand command;

    @Mock
    private ResourceLoader resourceLoader;

    private ReadFolderService readFolderService;

    @BeforeEach
    void setUp() {
        readFolderService = new ReadFolderService(repository, command, resourceLoader);
    }

    @Test
    void process_WithValidFolder_ShouldReturnExpectedResult() throws Exception {
        // Arrange
        String args = "csv/";
        URL mockUrl = URI.create("file:/test/path").toURL();
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> expectedResult =
                Map.of(new CsvPaymentsInputFile(), new CsvPaymentsOutputFile());

        when(resourceLoader.getResource("csv/")).thenReturn(mockUrl);
        when(command.execute(any(CsvFolder.class))).thenReturn(expectedResult);

        // Act
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = readFolderService.process(args);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(command).execute(any(CsvFolder.class));
        verify(resourceLoader).getResource("csv/");
    }

    @Test
    void process_WhenResourceNotFound_ShouldThrowIllegalArgumentException() {
        // Arrange
        String args = "nonexistent/folder/";
        when(resourceLoader.getResource("nonexistent/folder/")).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readFolderService.process(args));
        assertEquals("Resource not found: nonexistent/folder/", exception.getMessage());
        verify(resourceLoader).getResource("nonexistent/folder/");
    }

    @Test
    void process_WithNoArguments_ShouldUseDefaultFolder() throws Exception {
        // Arrange
        URL mockUrl = URI.create("file:/test/path").toURL();
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> expectedResult =
                Map.of(new CsvPaymentsInputFile(), new CsvPaymentsOutputFile());

        when(resourceLoader.getResource("csv/")).thenReturn(mockUrl);
        when(command.execute(any(CsvFolder.class))).thenReturn(expectedResult);

        // Act
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = readFolderService.process("");

        // Assert
        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(command).execute(any(CsvFolder.class));
        verify(resourceLoader).getResource("csv/");
    }
}
