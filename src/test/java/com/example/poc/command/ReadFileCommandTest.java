package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private static final String MESSAGE = "Executing with %s";
    private CsvPaymentsFile csvPaymentsFile;

    @BeforeEach
    void setUp() {
//        MockitoAnnotations.initMocks(getClass());
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void execute(CapturedOutput output) {
        csvPaymentsFile = new CsvPaymentsFile(new File("src/test/resources/csv/test.csv"));
        ReadFileCommand readFileCommand = new ReadFileCommand();

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
        ReadFileCommand readFileCommand = new ReadFileCommand();

        // Call method
        Stream<PaymentRecord> recordStream = readFileCommand.execute(csvPaymentsFile);

        // Verify
        assertNull(recordStream);
        assertTrue(output.getOut().contains("src/test/resources/csv/nonexistent-file.csv (No such file or directory)"));
    }

    private Stream<PaymentRecord> getTestData() {

        List<PaymentRecord> verificationList = new ArrayList<>();

        verificationList.add(new PaymentRecord()
                .setAmount(new BigDecimal("123.50"))
                .setCsvId("1")
                .setFile(csvPaymentsFile)
                .setRecipient("Mariano")
                .setCurrency(Currency.getInstance("GBP")));
        verificationList.add(new PaymentRecord()
                .setAmount(new BigDecimal("456.60"))
                .setCsvId("2")
                .setFile(csvPaymentsFile)
                .setRecipient("Sergiu")
                .setCurrency(Currency.getInstance("USD")));
        verificationList.add(new PaymentRecord()
                .setAmount(new BigDecimal("789.25"))
                .setCsvId("3")
                .setFile(csvPaymentsFile)
                .setRecipient("Moldova")
                .setCurrency(Currency.getInstance("ARS")));

        return verificationList.stream();
    }
}
