package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.repository.CsvFolderRepository;
import com.example.poc.repository.CsvPaymentsFileRepository;
import com.example.poc.service.CsvPaymentsServiceImpl;
import org.junit.jupiter.api.AfterEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ReadFolderCommandTest {
    private static final String MESSAGE = "Executing with %s";

    @Mock
    CsvFolderRepository csvFolderRepository;

    @Mock
    CsvPaymentsFileRepository csvPaymentsFileRepository;

    @InjectMocks
    CsvPaymentsServiceImpl csvPaymentsService;

    private CsvFolder csvFolder;

    @BeforeEach
    void setUp() {
        csvFolder = new CsvFolder("src/test/resources/csv/");
        doReturn(csvFolder).when(csvFolderRepository).save(any());
        doReturn(getCsvPaymentsFile()).when(csvPaymentsFileRepository).save(any());
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void execute(CapturedOutput output) {
        ReadFolderCommand readFolderCommand = new ReadFolderCommand(csvPaymentsService);

        // Call method
        List<CsvPaymentsFile> fileList = readFolderCommand.execute(csvFolder);

        // verify log line at the beginning
        assertTrue(output.getOut().contains(String.format(MESSAGE, csvFolder)));
        // verify each record has the matching data
        assertEquals(fileList, List.of(getCsvPaymentsFile()));
    }

    private CsvPaymentsFile getCsvPaymentsFile() {
        return new CsvPaymentsFile(new File(csvFolder + "test.csv"));
    }
}