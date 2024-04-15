package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.service.ProcessFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ReadFolderCommandTest {
    // TODO move this assertion to the service test class
//    private static final String MESSAGE = "Executed with %s";

    @Mock
    ProcessFileService processFileService;

    @InjectMocks
    ReadFolderCommand readFolderCommand;

    private CsvFolder csvFolder;

    @BeforeEach
    void setUp() {
        csvFolder = new CsvFolder("src/test/resources/csv/");
    }

    @Test
    void execute() throws IOException {
        CsvPaymentsFile csvPaymentsFile = new CsvPaymentsFile(new File(STR."\{csvFolder}test.csv"));
        doReturn(csvPaymentsFile).when(processFileService).createCsvFile(new File(STR."\{csvFolder}test.csv"));
        CsvPaymentsFile csvPaymentsFile2 = new CsvPaymentsFile(new File(STR."\{csvFolder}test.output.csv"));
        doReturn(csvPaymentsFile2).when(processFileService).createCsvFile(new File(STR."\{csvFolder}test.output.csv"));
        // Call method
        Stream<CsvPaymentsFile> paymentsFileStream = readFolderCommand.execute(csvFolder);

        // TODO move this assertion to the service test class
        // Needs CapturedOutput output as a parameter
        // verify log line at the beginning
//        assertTrue(output.getOut().contains(String.format(MESSAGE, csvFolder)));

        // verify each record has the matching data
        assertEquals(paymentsFileStream.toList(), getCsvPaymentsFile());
    }

    @Test
    void executeWithIOException() throws IOException {
        doThrow(IOException.class).when(processFileService).createCsvFile(any(File.class));
        assertThrows(RuntimeException.class, () -> readFolderCommand.execute(csvFolder));
    }

    @Test
    void executeWithIOExceptionFromClose() throws IOException {
        CsvPaymentsFile csvPaymentsFile = mock(CsvPaymentsFile.class);
        doReturn(csvPaymentsFile).when(processFileService).createCsvFile(new File(STR."\{csvFolder}test.csv"));
        doThrow(IOException.class).when(csvPaymentsFile).close();

        assertThrows(RuntimeException.class, () -> readFolderCommand.execute(csvFolder));
    }

    private List<CsvPaymentsFile> getCsvPaymentsFile() throws IOException {
        return List.of(new CsvPaymentsFile(new File(STR."\{csvFolder}test.csv")), new CsvPaymentsFile(new File(STR."\{csvFolder}test.output.csv")));
    }
}
