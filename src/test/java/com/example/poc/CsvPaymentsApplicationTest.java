package com.example.poc;

import com.example.poc.domain.*;
import com.example.poc.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class CsvPaymentsApplicationTests {

    @Mock
    private ReadFolderService readFolderService;

    @Mock
    private ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService;

    @Mock
    private ProcessAckPaymentSentService processAckPaymentSentService;

    @Mock
    private SendPaymentRecordService sendPaymentRecordService;

    @Mock
    private ProcessPaymentOutputService processPaymentOutputService;

    @Mock
    private ProcessPaymentStatusService processPaymentStatusService;

    private CsvPaymentsApplication csvPaymentsApplication;

    @BeforeEach
    void setUp() {
        csvPaymentsApplication = new CsvPaymentsApplication(
                readFolderService,
                processCsvPaymentsInputFileService,
                processAckPaymentSentService,
                sendPaymentRecordService,
                processPaymentOutputService,
                processPaymentStatusService
        );
    }

    @Test
    void testRunWithValidInput() {
        // Arrange
        String[] args = new String[]{"testFolder"};
        CsvPaymentsInputFile inputFile = mock(CsvPaymentsInputFile.class);
        CsvPaymentsOutputFile outputFile = mock(CsvPaymentsOutputFile.class);
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> fileMap =
                Collections.singletonMap(inputFile, outputFile);
        AckPaymentSent ackPaymentSent = mock(AckPaymentSent.class);
        PaymentRecord paymentRecord = mock(PaymentRecord.class);
        PaymentStatus paymentStatus = mock(PaymentStatus.class);
        PaymentOutput paymentOutput = mock(PaymentOutput.class);


        // Mock the service responses
        when(readFolderService.process(args)).thenReturn(fileMap);
        when(processCsvPaymentsInputFileService.process(any()))
                .thenReturn(Stream.of(paymentRecord));
        when(sendPaymentRecordService.process(any())).thenReturn(ackPaymentSent);
        when(processAckPaymentSentService.process(any())).thenReturn(paymentStatus);
        when(processPaymentStatusService.process(any())).thenReturn(paymentOutput);
        when(processPaymentOutputService.process(any())).thenReturn(outputFile);

        // Act
        csvPaymentsApplication.run(args);

        // Assert
        verify(readFolderService).process(args);
        verify(processPaymentOutputService).initialiseFiles(fileMap);
        verify(processCsvPaymentsInputFileService).process(any());
        verify(processPaymentOutputService).closeFiles(fileMap.values());

        // Verify that all processing services were called
        verify(sendPaymentRecordService).process(any());
        verify(processAckPaymentSentService).process(any());
        verify(processPaymentStatusService).process(any());
        verify(processPaymentOutputService).process(any());
    }

    @Test
    void testRunWithEmptyFolder() {
        // Arrange
        String[] args = new String[]{"emptyFolder"};
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> emptyMap = Collections.emptyMap();

        when(readFolderService.process(args)).thenReturn(emptyMap);

        // Act
        csvPaymentsApplication.run(args);

        // Assert
        verify(readFolderService).process(args);
        verify(processPaymentOutputService).initialiseFiles(emptyMap);
        verify(processPaymentOutputService).closeFiles(emptyMap.values());
    }

    @Test
    void testGetCsvPaymentsOutputFilesList() {
        // Arrange
        PaymentRecord paymentRecord = mock(PaymentRecord.class);
        Stream<PaymentRecord> recordStream = Stream.of(paymentRecord);
        CsvPaymentsOutputFile outputFile = mock(CsvPaymentsOutputFile.class);
        PaymentStatus paymentStatus = mock(PaymentStatus.class);
        PaymentOutput paymentOutput = mock(PaymentOutput.class);
        AckPaymentSent ackPaymentSent = mock(AckPaymentSent.class);

        when(sendPaymentRecordService.process(paymentRecord)).thenReturn(ackPaymentSent);
        when(processAckPaymentSentService.process(any(AckPaymentSent.class))).thenReturn(paymentStatus);
        when(processPaymentStatusService.process(any(PaymentStatus.class))).thenReturn(paymentOutput);
        when(processPaymentOutputService.process(any(PaymentOutput.class))).thenReturn(outputFile);

        // Act
        List<CsvPaymentsOutputFile> result = csvPaymentsApplication
                .getCsvPaymentsOutputFilesList(recordStream);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sendPaymentRecordService).process(paymentRecord);
        verify(processAckPaymentSentService).process(ackPaymentSent);
        verify(processPaymentStatusService).process(paymentStatus);
        verify(processPaymentOutputService).process(paymentOutput);
    }

    @Test
    void testRunWithNullArguments() {
        // Arrange
        String[] args = null;
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> emptyMap = Collections.emptyMap();

        when(readFolderService.process(args)).thenReturn(emptyMap);

        // Act & Assert
        assertDoesNotThrow(() -> csvPaymentsApplication.run(args));
    }
}
