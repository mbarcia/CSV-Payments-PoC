package com.example.poc.service;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class OrchestratorServiceTest {

    @Inject
    OrchestratorService orchestratorService;

    @Mock
    HybridResourceLoader resourceLoader;

    @Mock
    MutinyProcessCsvPaymentsInputFileServiceGrpc.MutinyProcessCsvPaymentsInputFileServiceStub processCsvPaymentsInputFileService;

    @Mock
    MutinyProcessAckPaymentSentServiceGrpc.MutinyProcessAckPaymentSentServiceStub processAckPaymentSentService;

    @Mock
    MutinySendPaymentRecordServiceGrpc.MutinySendPaymentRecordServiceStub sendPaymentRecordService;

    @Mock
    MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub processCsvPaymentsOutputFileService;

    @Mock
    MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub processPaymentStatusService;

    @Mock
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Mock
    CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        orchestratorService.resourceLoader = resourceLoader;
        orchestratorService.processCsvPaymentsInputFileService = processCsvPaymentsInputFileService;
        orchestratorService.processAckPaymentSentService = processAckPaymentSentService;
        orchestratorService.sendPaymentRecordService = sendPaymentRecordService;
        orchestratorService.processCsvPaymentsOutputFileService = processCsvPaymentsOutputFileService;
        orchestratorService.processPaymentStatusService = processPaymentStatusService;
        orchestratorService.csvPaymentsOutputFileMapper = csvPaymentsOutputFileMapper;
        orchestratorService.csvPaymentsInputFileMapper = csvPaymentsInputFileMapper;
    }

    @Test
    public void testReadCsvFolder_whenFolderNotFound() {
        String nonExistentFolder = "non-existent-folder";
        Mockito.lenient().when(resourceLoader.getResource(nonExistentFolder)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            orchestratorService.readCsvFolder(nonExistentFolder);
        });
    }

    @Test
    public void testReadCsvFolder_whenPathIsNotDirectory() throws Exception {
        String filePath = "file.csv";
        File file = new File("src/test/resources/file.csv");
        file.createNewFile();
        URL fileUrl = file.toURI().toURL();
        Mockito.lenient().when(resourceLoader.getResource(filePath)).thenReturn(fileUrl);

        assertThrows(IllegalArgumentException.class, () -> {
            orchestratorService.readCsvFolder(filePath);
        });
    }

    @Test
    @SneakyThrows
    public void testReadCsvFolder_whenFolderIsEmpty() throws IllegalArgumentException {
        String emptyFolderPath = "empty-folder";
        File emptyDir = new File("src/test/resources/empty-folder");
        emptyDir.mkdirs();
        URL folderUrl = emptyDir.toURI().toURL();
        Mockito.lenient().when(resourceLoader.getResource(emptyFolderPath)).thenReturn(folderUrl);

        assertThrows(IllegalArgumentException.class, () -> orchestratorService.readCsvFolder(emptyFolderPath));
    }

    @Test
    public void testReadCsvFolder_whenFolderContainsCsvFiles() throws Exception {
        String csvFolderPath = "csv-folder-full";
        File csvDir = new File("src/test/resources/" + csvFolderPath);
        csvDir.mkdirs();
        new File(csvDir, "test1.csv").createNewFile();
        new File(csvDir, "test2.csv").createNewFile();
        new File(csvDir, "test3.csv").createNewFile();
        URL folderUrl = csvDir.toURI().toURL();
        Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);

        Map<CsvPaymentsInputFile, ?> result = orchestratorService.readCsvFolder(csvFolderPath);

        assertEquals(3, result.size());
    }

    @Test
    public void testIsThrottlingError_withResourceExhausted() {
        assertTrue(orchestratorService.isThrottlingError(new StatusRuntimeException(Status.RESOURCE_EXHAUSTED)));
    }

    @Test
    public void testIsThrottlingError_withUnavailable() {
        assertTrue(orchestratorService.isThrottlingError(new StatusRuntimeException(Status.UNAVAILABLE)));
    }

    @Test
    public void testIsThrottlingError_withAborted() {
        assertTrue(orchestratorService.isThrottlingError(new StatusRuntimeException(Status.ABORTED)));
    }

    @Test
    public void testIsThrottlingError_withDifferentStatus() {
        assertFalse(orchestratorService.isThrottlingError(new StatusRuntimeException(Status.INVALID_ARGUMENT)));
    }

    @Test
    public void testIsThrottlingError_withNonGrpcException() {
        assertFalse(orchestratorService.isThrottlingError(new RuntimeException()));
    }

//    @Test
//    public void testProcess_successful() throws URISyntaxException, IOException {
//        // Arrange
//        String csvFolderPath = "csv-folder";
//        File csvDir = new File("src/test/resources/csv-folder-success");
//        csvDir.mkdirs();
//        File testFile = new File(csvDir, "test.csv");
//        testFile.createNewFile();
//        URL folderUrl = csvDir.toURI().toURL();
//
//        Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);
//
//        CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(testFile);
//        InputCsvFileProcessingSvc.PaymentRecord paymentRecord = InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();
//        PaymentsProcessingSvc.AckPaymentSent ackPaymentSent = PaymentsProcessingSvc.AckPaymentSent.newBuilder().build();
//        PaymentsProcessingSvc.PaymentStatus paymentStatus = PaymentsProcessingSvc.PaymentStatus.newBuilder().build();
//        PaymentStatusSvc.PaymentOutput paymentOutput = PaymentStatusSvc.PaymentOutput.newBuilder().build();
//        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile grpcOutputFile = OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.newBuilder().build();
//        CsvPaymentsOutputFile expectedOutputFile = new CsvPaymentsOutputFile(csvDir + "test.csv");
//
//        Mockito.lenient().when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class))).thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
//        Mockito.lenient().when(processCsvPaymentsInputFileService.remoteProcess(any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class))).thenReturn(Multi.createFrom().item(paymentRecord));
//        Mockito.lenient().when(sendPaymentRecordService.remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class))).thenReturn(Uni.createFrom().item(ackPaymentSent));
//        Mockito.lenient().when(processAckPaymentSentService.remoteProcess(any(PaymentsProcessingSvc.AckPaymentSent.class))).thenReturn(Uni.createFrom().item(paymentStatus));
//        Mockito.lenient().when(processPaymentStatusService.remoteProcess(any(PaymentsProcessingSvc.PaymentStatus.class))).thenReturn(Uni.createFrom().item(paymentOutput));
//        Mockito.lenient().when(processCsvPaymentsOutputFileService.remoteProcess(any(Multi.class))).thenReturn(Uni.createFrom().item(grpcOutputFile));
//        Mockito.lenient().when(csvPaymentsOutputFileMapper.fromGrpc(any(OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.class))).thenReturn(expectedOutputFile);
//
//        // Act
//        orchestratorService.process(csvFolderPath).await().indefinitely();
//
//        // Assert
//        Mockito.verify(processCsvPaymentsInputFileService).remoteProcess(any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class));
//        Mockito.verify(sendPaymentRecordService).remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class));
//        Mockito.verify(processAckPaymentSentService).remoteProcess(any(PaymentsProcessingSvc.AckPaymentSent.class));
//        Mockito.verify(processPaymentStatusService).remoteProcess(any(PaymentsProcessingSvc.PaymentStatus.class));
//        Mockito.verify(processCsvPaymentsOutputFileService).remoteProcess(any(Multi.class));
//    }

    @Test
    public void testProcess_failure_sendPaymentRecord() throws URISyntaxException, IOException {
        // Arrange
        String csvFolderPath = "csv-folder";
        File csvDir = new File("src/test/resources/csv-folder");
        csvDir.mkdirs();
        File testFile = new File(csvDir, "test.csv");
        testFile.createNewFile();
        URL folderUrl = csvDir.toURI().toURL();

        Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);

        CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(testFile);
        InputCsvFileProcessingSvc.PaymentRecord paymentRecord = InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();

        Mockito.lenient().when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class))).thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
        Mockito.lenient().when(processCsvPaymentsInputFileService.remoteProcess(any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class))).thenReturn(Multi.createFrom().item(paymentRecord));
        Mockito.lenient().when(sendPaymentRecordService.remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class))).thenReturn(Uni.createFrom().failure(new StatusRuntimeException(Status.INTERNAL)));

        // Act & Assert
        assertThrows(StatusRuntimeException.class, () -> {
            orchestratorService.process(csvFolderPath).await().indefinitely();
        });
    }

    @Test
    public void testProcess_failure_processAckPaymentSent() throws URISyntaxException, IOException {
        // Arrange
        String csvFolderPath = "csv-folder";
        File csvDir = new File("src/test/resources/csv-folder");
        csvDir.mkdirs();
        File testFile = new File(csvDir, "test.csv");
        testFile.createNewFile();
        URL folderUrl = csvDir.toURI().toURL();

        Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);

        CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(testFile);
        InputCsvFileProcessingSvc.PaymentRecord paymentRecord = InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();
        PaymentsProcessingSvc.AckPaymentSent ackPaymentSent = PaymentsProcessingSvc.AckPaymentSent.newBuilder().build();

        Mockito.lenient().when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class))).thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
        Mockito.lenient().when(processCsvPaymentsInputFileService.remoteProcess(any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class))).thenReturn(Multi.createFrom().item(paymentRecord));
        Mockito.lenient().when(sendPaymentRecordService.remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class))).thenReturn(Uni.createFrom().item(ackPaymentSent));
        Mockito.lenient().when(processAckPaymentSentService.remoteProcess(any(PaymentsProcessingSvc.AckPaymentSent.class))).thenReturn(Uni.createFrom().failure(new StatusRuntimeException(Status.INTERNAL)));

        // Act & Assert
        assertThrows(StatusRuntimeException.class, () -> {
            orchestratorService.process(csvFolderPath).await().indefinitely();
        });
    }

    @Test
    public void testProcess_failure_processPaymentStatus() throws URISyntaxException, IOException {
        // Arrange
        String csvFolderPath = "csv-folder";
        File csvDir = new File("src/test/resources/csv-folder");
        csvDir.mkdirs();
        File testFile = new File(csvDir, "test.csv");
        testFile.createNewFile();
        URL folderUrl = csvDir.toURI().toURL();

        Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);

        CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(testFile);
        InputCsvFileProcessingSvc.PaymentRecord paymentRecord = InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();
        PaymentsProcessingSvc.AckPaymentSent ackPaymentSent = PaymentsProcessingSvc.AckPaymentSent.newBuilder().build();
        PaymentsProcessingSvc.PaymentStatus paymentStatus = PaymentsProcessingSvc.PaymentStatus.newBuilder().build();

        Mockito.lenient().when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class))).thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
        Mockito.lenient().when(processCsvPaymentsInputFileService.remoteProcess(any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class))).thenReturn(Multi.createFrom().item(paymentRecord));
        Mockito.lenient().when(sendPaymentRecordService.remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class))).thenReturn(Uni.createFrom().item(ackPaymentSent));
        Mockito.lenient().when(processAckPaymentSentService.remoteProcess(any(PaymentsProcessingSvc.AckPaymentSent.class))).thenReturn(Uni.createFrom().item(paymentStatus));
        Mockito.lenient().when(processPaymentStatusService.remoteProcess(any(PaymentsProcessingSvc.PaymentStatus.class))).thenReturn(Uni.createFrom().failure(new StatusRuntimeException(Status.INTERNAL)));

        // Act & Assert
        assertThrows(StatusRuntimeException.class, () -> {
            orchestratorService.process(csvFolderPath).await().indefinitely();
        });
    }

//    @Test
//    public void testProcess_retry() throws URISyntaxException, IOException {
//        // Arrange
//        String csvFolderPath = "src/test/resources/csv-folder-retry";
//        File csvDir = new File(csvFolderPath);
//        csvDir.mkdirs();
//        File testFile = new File(csvDir, "test.csv");
//        testFile.createNewFile();
//        URL folderUrl = csvDir.toURI().toURL();
//
//        Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);
//
//        CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(testFile);
//        InputCsvFileProcessingSvc.PaymentRecord paymentRecord = InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();
//        PaymentsProcessingSvc.AckPaymentSent ackPaymentSent = PaymentsProcessingSvc.AckPaymentSent.newBuilder().build();
//        PaymentsProcessingSvc.PaymentStatus paymentStatus = PaymentsProcessingSvc.PaymentStatus.newBuilder().build();
//        PaymentStatusSvc.PaymentOutput paymentOutput = PaymentStatusSvc.PaymentOutput.newBuilder().build();
//        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile grpcOutputFile = OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.newBuilder().build();
//        CsvPaymentsOutputFile expectedOutputFile = new CsvPaymentsOutputFile(csvFolderPath + "/test.csv");
//
//        Mockito.lenient().when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class))).thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
//        Mockito.lenient().when(processCsvPaymentsInputFileService.remoteProcess(any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class))).thenReturn(Multi.createFrom().item(paymentRecord));
//        Mockito.lenient().when(sendPaymentRecordService.remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class)))
//                .thenReturn(Uni.createFrom().failure(new StatusRuntimeException(Status.RESOURCE_EXHAUSTED)))
//                .thenReturn(Uni.createFrom().failure(new StatusRuntimeException(Status.RESOURCE_EXHAUSTED)))
//                .thenReturn(Uni.createFrom().item(ackPaymentSent));
//        Mockito.lenient().when(processAckPaymentSentService.remoteProcess(any(PaymentsProcessingSvc.AckPaymentSent.class))).thenReturn(Uni.createFrom().item(paymentStatus));
//        Mockito.lenient().when(processPaymentStatusService.remoteProcess(any(PaymentsProcessingSvc.PaymentStatus.class))).thenReturn(Uni.createFrom().item(paymentOutput));
//        Mockito.lenient().when(processCsvPaymentsOutputFileService.remoteProcess(any(Multi.class))).thenReturn(Uni.createFrom().item(grpcOutputFile));
//        Mockito.lenient().when(csvPaymentsOutputFileMapper.fromGrpc(any(OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.class))).thenReturn(expectedOutputFile);
//
//        // Act
//        orchestratorService.process(csvFolderPath).await().indefinitely();
//
//        // Assert
//        Mockito.verify(sendPaymentRecordService, times(3)).remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class));
//
//        // Cleanup
//        testFile.delete();
//    }
}
