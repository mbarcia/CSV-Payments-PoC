package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.service.ProcessRecordService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.repository.CrudRepository;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ProcessFileCommandTest {
    private static final String MESSAGE = "Executed with %s";

    @Mock
    ProcessRecordService processRecordService;

    ProcessFileCommand processFileCommand;

    CrudRepository<CsvPaymentsFile, Long> repository = mock(CrudRepository.class);

    @BeforeEach
    void setUp() {
        when(repository.save(any(CsvPaymentsFile.class))).thenReturn(null);
        processFileCommand = new ProcessFileCommand(processRecordService);
    }

    @Test
    void execute(CapturedOutput output) throws IOException {
        CsvPaymentsFile csvPaymentsFile = new CsvPaymentsFile(new File("src/test/resources/csv/test.csv"));
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

            // verify log line at the beginning
//            assertTrue(output.getOut().contains(String.format(MESSAGE, csvPaymentsFile)));

            // TODO actually mock the 3 outputs
//            assertEquals(paymentOutputList, testPaymentOutputList);
        }
    }

    @Test
    void executeFileNotFound(CapturedOutput output) {
        Exception exception = assertThrows(FileNotFoundException.class, () -> processFileCommand.execute(new CsvPaymentsFile(new File("src/test/resources/csv/nonexistent-file.csv"))));
        assertTrue(exception.getMessage().contains("src/test/resources/csv/nonexistent-file.csv (No such file or directory)"));
    }
}
