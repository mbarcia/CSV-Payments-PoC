package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentOutputCommandTest {

    @Mock
    private PaymentOutput paymentOutput;

    @Mock
    private PaymentRecord paymentRecord;

    @Mock
    private CsvPaymentsOutputFile csvPaymentsOutputFile;

    @Mock
    private StatefulBeanToCsv<PaymentOutput> sbc;

    private ProcessPaymentOutputCommand command;

    @BeforeEach
    void setUp() {
        command = new ProcessPaymentOutputCommand();
    }

    @Test
    void execute_ShouldWriteToFileAndReturnCsvFile_WhenSuccessful() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        // Arrange
        when(paymentOutput.getPaymentRecord()).thenReturn(paymentRecord);
        when(paymentRecord.getCsvPaymentsOutputFile()).thenReturn(csvPaymentsOutputFile);
        when(csvPaymentsOutputFile.getSbc()).thenReturn(sbc);
        doNothing().when(sbc).write(paymentOutput);

        // Act
        CsvPaymentsOutputFile result = command.execute(paymentOutput);

        // Assert
        assertNotNull(result);
        assertEquals(csvPaymentsOutputFile, result);
        verify(sbc).write(paymentOutput);
        verify(paymentOutput).getPaymentRecord();
        verify(paymentRecord).getCsvPaymentsOutputFile();
        verify(csvPaymentsOutputFile).getSbc();
    }

    @Test
    void execute_ShouldThrowRuntimeException_WhenCsvRequiredFieldEmptyExceptionOccurs() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        // Arrange
        when(paymentOutput.getPaymentRecord()).thenReturn(paymentRecord);
        when(paymentRecord.getCsvPaymentsOutputFile()).thenReturn(csvPaymentsOutputFile);
        when(csvPaymentsOutputFile.getSbc()).thenReturn(sbc);
        doThrow(new CsvRequiredFieldEmptyException()).when(sbc).write(paymentOutput);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> command.execute(paymentOutput));
    }

    @Test
    void execute_ShouldThrowRuntimeException_WhenCsvDataTypeMismatchExceptionOccurs() throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        // Arrange
        when(paymentOutput.getPaymentRecord()).thenReturn(paymentRecord);
        when(paymentRecord.getCsvPaymentsOutputFile()).thenReturn(csvPaymentsOutputFile);
        when(csvPaymentsOutputFile.getSbc()).thenReturn(sbc);
        doThrow(new CsvDataTypeMismatchException()).when(sbc).write(paymentOutput);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> command.execute(paymentOutput));
    }
}
