package com.example.poc.service;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentRecord;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestratorServiceTest {

    @Mock
    private HybridResourceLoader resourceLoader;

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

    @InjectMocks
    private OrchestratorService orchestratorService;

    private static final String TEST_FOLDER_PATH = "test-csv/";
    private URL testUrl;
    private CsvPaymentsInputFile testInputFile;
    private CsvPaymentsOutputFile testOutputFile;
    private PaymentRecord testPaymentRecord;

    @BeforeEach
    void setUp() throws Exception {
        // Setup test data
        testUrl = new URI("file:/test-csv/").toURL();
        new CsvFolder("/test-csv/");
        testInputFile = new CsvPaymentsInputFile();
        testInputFile.setFilepath("test.csv");
        testOutputFile = new CsvPaymentsOutputFile();
        testOutputFile.setFilepath("test.output.csv");
        testPaymentRecord = new PaymentRecord();
        testPaymentRecord.setCsvId("123456");
    }

    @SneakyThrows
    @Test
    void process_happyPath() {
        // Given
        List<URL> csvFiles = List.of(new URI("file:/test-csv/test.csv").toURL());
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> fileMap = Map.of(testInputFile, testOutputFile);

        when(resourceLoader.listResources(TEST_FOLDER_PATH)).thenReturn(csvFiles);
        when(resourceLoader.getResource(TEST_FOLDER_PATH)).thenReturn(testUrl);
        when(readFolderService.process(any(CsvFolder.class))).thenReturn(fileMap);
        when(processCsvPaymentsInputFileService.process(testInputFile))
                .thenReturn(Stream.of(testPaymentRecord));
        when(sendPaymentRecordService.process(testPaymentRecord)).thenReturn(null); // Return appropriate values
        when(processAckPaymentSentService.process(any())).thenReturn(null); // Return appropriate values
        when(processPaymentStatusService.process(any())).thenReturn(null); // Return appropriate values
        when(processPaymentOutputService.process(any())).thenReturn(testOutputFile);

        // When
        orchestratorService.process(TEST_FOLDER_PATH);

        // Then
        verify(resourceLoader).listResources(TEST_FOLDER_PATH);
        verify(resourceLoader).getResource(TEST_FOLDER_PATH);
        verify(readFolderService).process(argThat(folder ->
        {
            try {
                return folder.getFolderPath().equals(testUrl.toURI().getPath());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }));
        verify(processPaymentOutputService).initialiseFiles(fileMap);
        verify(processCsvPaymentsInputFileService).process(testInputFile);
        verify(processPaymentOutputService).closeFiles(fileMap.values());

        // Verify print methods were called
        verify(readFolderService).print();
        verify(processCsvPaymentsInputFileService).print();
        verify(sendPaymentRecordService).print();
        verify(processAckPaymentSentService).print();
        verify(processPaymentStatusService).print();
        verify(processPaymentOutputService).print();
    }

    @Test
    void process_emptyFolder() throws URISyntaxException {
        // Given
        List<URL> csvFiles = Collections.emptyList();

        when(resourceLoader.listResources(TEST_FOLDER_PATH)).thenReturn(csvFiles);
        when(resourceLoader.getResource(TEST_FOLDER_PATH)).thenReturn(testUrl);
        when(readFolderService.process(any(CsvFolder.class))).thenReturn(Collections.emptyMap());

        // When
        orchestratorService.process(TEST_FOLDER_PATH);

        // Then
        verify(resourceLoader).listResources(TEST_FOLDER_PATH);
        verify(resourceLoader).diagnoseResourceAccess(TEST_FOLDER_PATH);
        verify(resourceLoader).getResource(TEST_FOLDER_PATH);
        verify(readFolderService).process(any(CsvFolder.class));
        verify(processPaymentOutputService).initialiseFiles(Collections.emptyMap());
        verify(processPaymentOutputService).closeFiles(Collections.emptySet());

        // Verify print methods were still called
        verify(readFolderService).print();
        verify(processCsvPaymentsInputFileService).print();
        verify(sendPaymentRecordService).print();
        verify(processAckPaymentSentService).print();
        verify(processPaymentStatusService).print();
        verify(processPaymentOutputService).print();
    }

    @Test
    void process_resourceNotFound() {
        // Given
        when(resourceLoader.listResources(TEST_FOLDER_PATH)).thenReturn(Collections.emptyList());
        when(resourceLoader.getResource(TEST_FOLDER_PATH)).thenReturn(null);

        // When/Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orchestratorService.process(TEST_FOLDER_PATH)
        );

        assertEquals("Resource not found: " + TEST_FOLDER_PATH, exception.getMessage());
        verify(resourceLoader).listResources(TEST_FOLDER_PATH);
        verify(resourceLoader).diagnoseResourceAccess(TEST_FOLDER_PATH);
        verify(resourceLoader).getResource(TEST_FOLDER_PATH);
        verify(readFolderService, never()).process(any(CsvFolder.class));
    }

    @Test
    void getCsvPaymentsOutputFilesList_parallelProcessing() {
        // Given
        PaymentRecord record1 = new PaymentRecord();
        record1.setCsvId("123");
        PaymentRecord record2 = new PaymentRecord();
        record2.setCsvId("456");

        Stream<PaymentRecord> recordStream = Stream.of(record1, record2);

        when(sendPaymentRecordService.process(any(PaymentRecord.class))).thenReturn(null);
        when(processAckPaymentSentService.process(any())).thenReturn(null);
        when(processPaymentStatusService.process(any())).thenReturn(null);
        when(processPaymentOutputService.process(any())).thenReturn(testOutputFile);

        // When
        List<CsvPaymentsOutputFile> result = orchestratorService.getCsvPaymentsOutputFilesList(recordStream);

        // Then
        assertEquals(2, result.size());
        assertEquals(testOutputFile, result.get(0));
        assertEquals(testOutputFile, result.get(1));

        // Verify each record was processed
        verify(sendPaymentRecordService, times(2)).process(any(PaymentRecord.class));
        verify(processAckPaymentSentService, times(2)).process(any());
        verify(processPaymentStatusService, times(2)).process(any());
        verify(processPaymentOutputService, times(2)).process(any());
    }

    @Test
    void getCsvPaymentsOutputFilesList_handlesExecutionException() {
        // Given
        PaymentRecord record = new PaymentRecord();
        record.setCsvId("123");

        Stream<PaymentRecord> recordStream = Stream.of(record);

        when(sendPaymentRecordService.process(any(PaymentRecord.class)))
                .thenThrow(new RuntimeException("Test exception"));

        // When/Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orchestratorService.getCsvPaymentsOutputFilesList(recordStream)
        );

        assertEquals("Error processing payment record", exception.getMessage());
        assertInstanceOf(ExecutionException.class, exception.getCause());
        assertEquals("java.lang.RuntimeException: Test exception", exception.getCause().getMessage());
    }

    @Test
    void getSingleRecordStream() {
        // Given
        Set<CsvPaymentsInputFile> inputFiles = new HashSet<>();
        CsvPaymentsInputFile file1 = new CsvPaymentsInputFile();
        file1.setFilepath("file1.csv");
        CsvPaymentsInputFile file2 = new CsvPaymentsInputFile();
        file2.setFilepath("file2.csv");
        inputFiles.add(file1);
        inputFiles.add(file2);

        PaymentRecord record1 = new PaymentRecord();
        record1.setCsvId("111");
        PaymentRecord record2 = new PaymentRecord();
        record2.setCsvId("222");

        when(processCsvPaymentsInputFileService.process(file1))
                .thenReturn(Stream.of(record1));
        when(processCsvPaymentsInputFileService.process(file2))
                .thenReturn(Stream.of(record2));

        // When
        Stream<Stream<PaymentRecord>> result = orchestratorService
                .getSingleRecordStream(inputFiles);

        // Then
        List<List<PaymentRecord>> collectedResults = result
                .map(Stream::toList)
                .toList();

        assertEquals(2, collectedResults.size());

        // Check each stream has the expected records
        boolean foundRecord1 = false;
        boolean foundRecord2 = false;

        for (List<PaymentRecord> records : collectedResults) {
            assertEquals(1, records.size()); // Each file has one record
            PaymentRecord record = records.getFirst();
            if ("111".equals(record.getCsvId())) {
                foundRecord1 = true;
            } else if ("222".equals(record.getCsvId())) {
                foundRecord2 = true;
            }
        }

        assertTrue(foundRecord1, "Should contain record from file1");
        assertTrue(foundRecord2, "Should contain record from file2");

        verify(processCsvPaymentsInputFileService).process(file1);
        verify(processCsvPaymentsInputFileService).process(file2);
    }
}
