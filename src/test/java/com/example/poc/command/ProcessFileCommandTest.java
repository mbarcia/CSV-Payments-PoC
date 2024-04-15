package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.service.ProcessFileService;
import com.example.poc.service.ProcessRecordService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ProcessFileCommandTest {
    // TODO move this to the service test class
    //  private static final String MESSAGE = "Executed with %s";

    @Mock
    ProcessRecordService processRecordService;

    @Mock
    ProcessFileService processFileService;

    ProcessFileCommand processFileCommand;

    @BeforeEach
    void setUp() {
        processFileCommand = new ProcessFileCommand(processRecordService);
    }

    @Test
    void execute() throws IOException {
        try (CsvPaymentsFile csvPaymentsFile = new CsvPaymentsFile(new File("src/test/resources/csv/test.csv"))) {
//            doReturn(csvPaymentsFile).when(processFileService).createCsvFile(any(File.class));
            try (Reader reader = new BufferedReader(new FileReader("src/test/resources/csv/test.output.csv"))) {
                CsvToBean<PaymentOutput> cb = new CsvToBeanBuilder<PaymentOutput>(reader)
                        .withType(PaymentOutput.class)
                        .withSeparator(',')
                        .withQuoteChar('\'')
                        .withIgnoreQuotations(true)
                        .withIgnoreLeadingWhiteSpace(true)
                        .withIgnoreEmptyLine(true)
                        .build();

//            List<PaymentOutput> testPaymentOutputList = cb.parse();

                // Call method
//            List<PaymentOutput> paymentOutputList = processFileCommand.execute(csvPaymentsFile);
            }

            // TODO move this to the service test class
            // verify log line at the beginning
//            assertTrue(output.getOut().contains(String.format(MESSAGE, csvPaymentsFile)));

            // TODO actually mock the 3 outputs
//            assertEquals(paymentOutputList, testPaymentOutputList);
        }
    }

    @Test
    void executeFileNotFound() {
        Exception exception = assertThrows(FileNotFoundException.class, () -> processFileCommand.execute(new CsvPaymentsFile(new File("src/test/resources/csv/nonexistent-file.csv"))));
        assertTrue(exception.getMessage().contains("src/test/resources/csv/nonexistent-file.csv (No such file or directory)"));
    }
}
