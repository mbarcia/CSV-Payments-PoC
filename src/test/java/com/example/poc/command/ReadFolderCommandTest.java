package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.repository.CrudRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ReadFolderCommandTest {
    private static final String MESSAGE = "Executed with %s";

    ReadFolderCommand readFolderCommand;

    CrudRepository<CsvFolder, Long> csvFolderRepository = mock(CrudRepository.class);

    private CsvFolder csvFolder;

    @BeforeEach
    void setUp() {
        readFolderCommand = new ReadFolderCommand();
        csvFolder = new CsvFolder("src/test/resources/csv/");
//        when(csvFolderRepository.save(any(CsvFolder.class))).thenReturn(null);
    }

    @Test
    void execute(CapturedOutput output) throws IOException {
        // Call method
        Stream<CsvPaymentsFile> paymentsFileStream = readFolderCommand.execute(csvFolder);

        // verify log line at the beginning
//        assertTrue(output.getOut().contains(String.format(MESSAGE, csvFolder)));
        // verify each record has the matching data
        assertEquals(paymentsFileStream.toList(), getCsvPaymentsFile());
    }

    private List<CsvPaymentsFile> getCsvPaymentsFile() throws IOException {
        return List.of(new CsvPaymentsFile(new File(STR."\{csvFolder}test.csv")), new CsvPaymentsFile(new File(STR."\{csvFolder}test.output.csv")));
    }
}
