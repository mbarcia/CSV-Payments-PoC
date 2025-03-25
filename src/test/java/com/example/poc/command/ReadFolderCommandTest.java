package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.service.ProcessCsvPaymentsInputFileService;
import com.example.poc.service.ProcessPaymentOutputService;
import com.example.poc.service.FileListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadFolderCommandTest {

    @Mock
    private ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService;

    @Mock
    private ProcessPaymentOutputService processPaymentOutputService;

    @Mock
    private FileListingService fileListingService;

    @Mock
    private CsvFolder csvFolder;

    private ReadFolderCommand readFolderCommand;

    @BeforeEach
    void setUp() {
        readFolderCommand = new ReadFolderCommand(
                processCsvPaymentsInputFileService,
                processPaymentOutputService,
                fileListingService
        );
    }

    @Test
    void execute_WithValidFiles_ShouldProcessAllFiles() throws IOException {
        // Arrange
        String folderPath = "/test/folder";
        File mockFile1 = new File(folderPath + "/test1.csv");
        File mockFile2 = new File(folderPath + "/test2.csv");
        File[] mockFiles = {mockFile1, mockFile2};

        CsvPaymentsInputFile mockInputFile1 = mock(CsvPaymentsInputFile.class);
        CsvPaymentsInputFile mockInputFile2 = mock(CsvPaymentsInputFile.class);
        CsvPaymentsOutputFile mockOutputFile1 = mock(CsvPaymentsOutputFile.class);
        CsvPaymentsOutputFile mockOutputFile2 = mock(CsvPaymentsOutputFile.class);

        // Setup mocks
        when(csvFolder.getFolderPath()).thenReturn(folderPath);
        when(fileListingService.listCsvFiles(folderPath)).thenReturn(mockFiles);

        when(processCsvPaymentsInputFileService.createCsvFile(mockFile1)).thenReturn(mockInputFile1);
        when(processCsvPaymentsInputFileService.createCsvFile(mockFile2)).thenReturn(mockInputFile2);
        when(processPaymentOutputService.createCsvFile(mockInputFile1)).thenReturn(mockOutputFile1);
        when(processPaymentOutputService.createCsvFile(mockInputFile2)).thenReturn(mockOutputFile2);

        // Act
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = readFolderCommand.execute(csvFolder);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(mockInputFile1));
        assertTrue(result.containsKey(mockInputFile2));
        assertEquals(mockOutputFile1, result.get(mockInputFile1));
        assertEquals(mockOutputFile2, result.get(mockInputFile2));

        verify(fileListingService).listCsvFiles(folderPath);
        verify(mockInputFile1).setCsvFolder(csvFolder);
        verify(mockInputFile2).setCsvFolder(csvFolder);
    }

    @Test
    void execute_WithIOException_ShouldSkipFailedFile() throws IOException {
        // Arrange
        String folderPath = "/test/folder";
        File mockFile1 = new File(folderPath + "/test1.csv");
        File mockFile2 = new File(folderPath + "/test2.csv");
        File[] mockFiles = {mockFile1, mockFile2};

        CsvPaymentsInputFile mockInputFile1 = mock(CsvPaymentsInputFile.class);
        CsvPaymentsInputFile mockInputFile2 = mock(CsvPaymentsInputFile.class);
        CsvPaymentsOutputFile mockOutputFile1 = mock(CsvPaymentsOutputFile.class);

        // Setup mocks
        when(csvFolder.getFolderPath()).thenReturn(folderPath);
        when(fileListingService.listCsvFiles(folderPath)).thenReturn(mockFiles);

        when(processCsvPaymentsInputFileService.createCsvFile(mockFile1)).thenReturn(mockInputFile1);
        when(processCsvPaymentsInputFileService.createCsvFile(mockFile2)).thenReturn(mockInputFile2);
        when(processPaymentOutputService.createCsvFile(mockInputFile1)).thenReturn(mockOutputFile1);
        when(processPaymentOutputService.createCsvFile(mockInputFile2)).thenThrow(new IOException("Test exception"));

        // Act
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = readFolderCommand.execute(csvFolder);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(mockInputFile1));
        assertEquals(mockOutputFile1, result.get(mockInputFile1));

        verify(fileListingService).listCsvFiles(folderPath);
        verify(mockInputFile1).setCsvFolder(csvFolder);
        verify(mockInputFile2).setCsvFolder(csvFolder);
    }

    @Test
    void execute_WithEmptyFolder_ShouldReturnEmptyMap() {
        // Arrange
        String folderPath = "/test/empty/folder";
        when(csvFolder.getFolderPath()).thenReturn(folderPath);
        when(fileListingService.listCsvFiles(folderPath)).thenReturn(new File[0]);

        // Act
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = readFolderCommand.execute(csvFolder);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(fileListingService).listCsvFiles(folderPath);
    }
}
