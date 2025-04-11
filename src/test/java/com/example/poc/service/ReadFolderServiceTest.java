package com.example.poc.service;

import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.repository.CsvFolderRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadFolderServiceTest {

    @Mock
    private CsvFolderRepository repository;

    @Mock
    private ReadFolderCommand command;

    private ReadFolderService service;

    @BeforeEach
    void setUp() {
        service = new ReadFolderService(repository, command);
    }

    @Test
    void process_shouldSaveToRepositoryAndExecuteCommand() {
        // Given
        CsvFolder csvFolder = new CsvFolder();
        csvFolder.setFolderPath("/test/path");

        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> expectedResult = new HashMap<>();
        CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile();
        inputFile.setFilepath("test.csv");
        CsvPaymentsOutputFile outputFile = new CsvPaymentsOutputFile();
        outputFile.setFilepath("test.output.csv");
        expectedResult.put(inputFile, outputFile);

        when(command.execute(csvFolder)).thenReturn(expectedResult);

        // When
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = service.process(csvFolder);

        // Then
        verify(repository).persist(csvFolder);
        verify(command).execute(csvFolder);
        assertEquals(expectedResult, result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(inputFile));
        assertEquals(outputFile, result.get(inputFile));
    }

    @Test
    void process_withNullInput_shouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> service.process(null));
    }

    @Test
    void process_whenCommandReturnsEmpty_shouldReturnEmptyMap() {
        // Given
        CsvFolder csvFolder = new CsvFolder();
        csvFolder.setFolderPath("/test/path");

        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> emptyMap = new HashMap<>();

        when(command.execute(csvFolder)).thenReturn(emptyMap);

        // When
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = service.process(csvFolder);

        // Then
        verify(repository).persist(csvFolder);
        verify(command).execute(csvFolder);
        assertTrue(result.isEmpty());
    }

    @Test
    void print_shouldCallFindAllOnRepository() {
        // Given
        CsvFolder folder1 = new CsvFolder();
        folder1.setFolderPath("/test/path1");
        CsvFolder folder2 = new CsvFolder();
        folder2.setFolderPath("/test/path2");

        doReturn(List.of(folder1, folder2)).when(repository).listAll();
//        when(repository.findAll()).thenReturn(java.util.List.of(folder1, folder2));

        // When
        service.print();

        // Then
        verify(repository).listAll();
    }

    @Test
    void getRepository_shouldReturnRepository() {
        // When
        PanacheRepository<CsvFolder> result = service.getRepository();

        // Then
        assertNotNull(result);
        assertEquals(repository, result);
    }

    @Test
    void getCommand_shouldReturnCommand() {
        // When
        ReadFolderCommand result = (ReadFolderCommand) service.getCommand();

        // Then
        assertNotNull(result);
        assertEquals(command, result);
    }

    @Test
    void process_whenRepositoryThrowsException_shouldPropagateException() {
        // Given
        CsvFolder csvFolder = new CsvFolder();
        csvFolder.setFolderPath("/test/path");

        RuntimeException expectedException = new RuntimeException("Test exception");
        doThrow(expectedException).when(repository).persist(csvFolder);

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> service.process(csvFolder));
        assertEquals("Test exception", exception.getMessage());
        verify(repository).persist(csvFolder);
        verify(command, never()).execute(any());
    }

    @Test
    void process_whenCommandThrowsException_shouldPropagateException() {
        // Given
        CsvFolder csvFolder = new CsvFolder();
        csvFolder.setFolderPath("/test/path");

        RuntimeException expectedException = new RuntimeException("Command exception");
        when(command.execute(csvFolder)).thenThrow(expectedException);

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () -> service.process(csvFolder));
        assertEquals("Command exception", exception.getMessage());
        verify(repository).persist(csvFolder);
        verify(command).execute(csvFolder);
    }
}
