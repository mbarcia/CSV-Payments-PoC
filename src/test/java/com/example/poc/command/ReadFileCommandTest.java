package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.service.CsvPaymentsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ReadFileCommandTest {
    private static final String MESSAGE = "Executed with %s";
    @InjectMocks
    ReadFileCommand readFileCommand;
    private CsvPaymentsFile csvPaymentsFile;
    @Mock
    private CsvPaymentsServiceImpl csvPaymentsService;

    @Test
    void execute(CapturedOutput output) {
        csvPaymentsFile = new CsvPaymentsFile(new File("src/test/resources/csv/test.csv"));

        // Call method
        Stream<PaymentRecord> recordStream = readFileCommand.execute(csvPaymentsFile);

        // verify log line at the beginning
        assertTrue(output.getOut().contains(String.format(MESSAGE, csvPaymentsFile)));
        // verify each record has the matching data
        assertEquals(recordStream.toList(), getTestData().toList());
    }

    @Test
    void executeFileNotFound(CapturedOutput output) {
        csvPaymentsFile = new CsvPaymentsFile(new File("src/test/resources/csv/nonexistent-file.csv"));

        // Call method
        Stream<PaymentRecord> recordStream = readFileCommand.execute(csvPaymentsFile);

        // Verify
        assertNull(recordStream);
        assertTrue(output.getOut().contains("src/test/resources/csv/nonexistent-file.csv (No such file or directory)"));
    }

    private Stream<PaymentRecord> getTestData() {
        List<PaymentRecord> verificationList = new ArrayList<>();

        verificationList.add(new PaymentRecord("1", "Mariano", new BigDecimal("123.50"), Currency.getInstance("GBP"))
                .setCsvPaymentsFile(csvPaymentsFile));
        verificationList.add(new PaymentRecord("2", "Sergiu", new BigDecimal("456.60"), Currency.getInstance("USD"))
                .setCsvPaymentsFile(csvPaymentsFile));
        verificationList.add(new PaymentRecord("3", "Moldova", new BigDecimal("789.25"), Currency.getInstance("ARS"))
                .setCsvPaymentsFile(csvPaymentsFile));

        return verificationList.stream();
    }
}
