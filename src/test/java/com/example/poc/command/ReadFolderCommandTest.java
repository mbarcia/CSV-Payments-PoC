package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.service.CsvPaymentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ReadFolderCommandTest {
    private static final String MESSAGE = "Executed with %s";

    @InjectMocks
    ReadFolderCommand readFolderCommand;
    @Mock
    CsvPaymentsServiceImpl csvPaymentsService;
    private CsvFolder csvFolder;

    @BeforeEach
    void setUp() {
        csvFolder = new CsvFolder("src/test/resources/csv/");
        doReturn(csvFolder).when(csvPaymentsService).persist(any(CsvFolder.class));
    }

    @Test
    void execute(CapturedOutput output) {
        // Call method
        Stream<CsvPaymentsFile> paymentsFileStream = readFolderCommand.execute(csvFolder);

        // verify log line at the beginning
        assertTrue(output.getOut().contains(String.format(MESSAGE, csvFolder)));
        // verify each record has the matching data
        assertEquals(paymentsFileStream.toList(), List.of(getCsvPaymentsFile()));
    }

    private CsvPaymentsFile getCsvPaymentsFile() {
        return new CsvPaymentsFile(new File(csvFolder + "test.csv"));
    }
}