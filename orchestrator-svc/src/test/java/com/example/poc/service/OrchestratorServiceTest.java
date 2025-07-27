/*
 * Copyright © 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.poc.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@QuarkusTest
public class OrchestratorServiceTest {

  // TODO

  @InjectMocks OrchestratorService orchestratorService;

  @Mock HybridResourceLoader resourceLoader;

  @Mock
  MutinyProcessCsvPaymentsInputFileServiceGrpc.MutinyProcessCsvPaymentsInputFileServiceStub
      processCsvPaymentsInputFileService;

  @Mock
  MutinyProcessAckPaymentSentServiceGrpc.MutinyProcessAckPaymentSentServiceStub
      processAckPaymentSentService;

  @Mock
  MutinySendPaymentRecordServiceGrpc.MutinySendPaymentRecordServiceStub sendPaymentRecordService;

  @Mock
  MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub
      processCsvPaymentsOutputFileService;

  @Mock
  MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub
      processPaymentStatusService;

  @Mock CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

  @Mock CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testReadCsvFolder_whenFolderNotFound() {
    String nonExistentFolder = "non-existent-folder";
    Mockito.lenient().when(resourceLoader.getResource(nonExistentFolder)).thenReturn(null);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          orchestratorService.readCsvFolder(nonExistentFolder);
        });
  }

  @Test
  public void testReadCsvFolder_whenPathIsNotDirectory(@TempDir Path tempDir) throws Exception {
    String filePath = "file.csv";
    File file = tempDir.resolve(filePath).toFile();
    file.createNewFile();
    URL fileUrl = file.toURI().toURL();
    Mockito.lenient().when(resourceLoader.getResource(filePath)).thenReturn(fileUrl);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          orchestratorService.readCsvFolder(filePath);
        });
  }

  @Test
  @SneakyThrows
  public void testReadCsvFolder_whenFolderIsEmpty(@TempDir Path tempDir) {
    String emptyFolderPath = "empty-folder";
    File emptyDir = tempDir.resolve(emptyFolderPath).toFile();
    emptyDir.mkdirs();
    URL folderUrl = emptyDir.toURI().toURL();
    Mockito.lenient().when(resourceLoader.getResource(emptyFolderPath)).thenReturn(folderUrl);

    assertEquals(Collections.emptyMap(), orchestratorService.readCsvFolder(emptyFolderPath));
  }

  @Test
  public void testReadCsvFolder_whenFolderContainsCsvFiles(@TempDir Path tempDir) throws Exception {
    String csvFolderPath = "csv-folder-full";
    Path csvDir = tempDir.resolve(csvFolderPath);
    Files.createDirectories(csvDir);
    Files.createFile(csvDir.resolve("test1.csv"));
    Files.createFile(csvDir.resolve("test2.csv"));
    Files.createFile(csvDir.resolve("test3.csv"));
    URL folderUrl = csvDir.toUri().toURL();
    Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);

    Map<CsvPaymentsInputFile, ?> result = orchestratorService.readCsvFolder(csvFolderPath);

    assertEquals(3, result.size());
  }

  @Test
  public void testIsThrottlingError_withResourceExhausted() {
    assertTrue(
        orchestratorService.isThrottlingError(
            new StatusRuntimeException(Status.RESOURCE_EXHAUSTED)));
  }

  @Test
  public void testIsThrottlingError_withUnavailable() {
    assertTrue(
        orchestratorService.isThrottlingError(new StatusRuntimeException(Status.UNAVAILABLE)));
  }

  @Test
  public void testIsThrottlingError_withAborted() {
    assertTrue(orchestratorService.isThrottlingError(new StatusRuntimeException(Status.ABORTED)));
  }

  @Test
  public void testIsThrottlingError_withDifferentStatus() {
    assertFalse(
        orchestratorService.isThrottlingError(new StatusRuntimeException(Status.INVALID_ARGUMENT)));
  }

  @Test
  public void testIsThrottlingError_withNonGrpcException() {
    assertFalse(orchestratorService.isThrottlingError(new RuntimeException()));
  }

  //  @Test
  //  @SneakyThrows
  //  public void testProcess_failure_sendPaymentRecord(@TempDir Path tempDir) {
  //    // Arrange
  //    String csvFolderPath = "csv-folder";
  //    Path csvDir = tempDir.resolve(csvFolderPath);
  //    Files.createDirectories(csvDir);
  //    File testFile = csvDir.resolve("test.csv").toFile();
  //    testFile.createNewFile();
  //    URL folderUrl = csvDir.toUri().toURL();
  //
  //    Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);
  //
  //    InputCsvFileProcessingSvc.PaymentRecord paymentRecord =
  //        InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();
  //
  //    Mockito.lenient()
  //        .when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class)))
  //        .thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
  //    Mockito.lenient()
  //        .when(
  //            processCsvPaymentsInputFileService.remoteProcess(
  //                any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class)))
  //        .thenReturn(Multi.createFrom().item(paymentRecord));
  //    Mockito.lenient()
  //        .when(
  //            sendPaymentRecordService.remoteProcess(
  //                any(InputCsvFileProcessingSvc.PaymentRecord.class)))
  //        .thenReturn(Uni.createFrom().failure(new StatusRuntimeException(Status.INTERNAL)));
  //
  //    // Act & Assert
  //    StatusRuntimeException thrown =
  //            assertThrows(
  //                    StatusRuntimeException.class,
  //                    () -> orchestratorService.process(csvFolderPath).await().indefinitely());
  //
  //    assertEquals(Status.INTERNAL.getCode(), thrown.getStatus().getCode());
  //  }

  //  @Test
  //  public void testProcess_failure_processAckPaymentSent(@TempDir Path tempDir) throws
  // URISyntaxException, IOException {
  //    // Arrange
  //    String csvFolderPath = "csv-folder";
  //    Path csvDir = tempDir.resolve(csvFolderPath);
  //    Files.createDirectories(csvDir);
  //    File testFile = csvDir.resolve("test.csv").toFile();
  //    testFile.createNewFile();
  //    URL folderUrl = csvDir.toUri().toURL();
  //
  //    Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);
  //
  //    InputCsvFileProcessingSvc.PaymentRecord paymentRecord =
  //        InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();
  //    PaymentsProcessingSvc.AckPaymentSent ackPaymentSent =
  //        PaymentsProcessingSvc.AckPaymentSent.newBuilder().build();
  //
  //    Mockito.lenient()
  //        .when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class)))
  //        .thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
  //    Mockito.lenient()
  //        .when(
  //            processCsvPaymentsInputFileService.remoteProcess(
  //                any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class)))
  //        .thenReturn(Multi.createFrom().item(paymentRecord));
  //    Mockito.lenient()
  //        .when(
  //            sendPaymentRecordService.remoteProcess(
  //                any(InputCsvFileProcessingSvc.PaymentRecord.class)))
  //        .thenReturn(Uni.createFrom().item(ackPaymentSent));
  //    Mockito.lenient()
  //        .when(
  //            processAckPaymentSentService.remoteProcess(
  //                any(PaymentsProcessingSvc.AckPaymentSent.class)))
  //        .thenReturn(Uni.createFrom().failure(new StatusRuntimeException(Status.INTERNAL)));
  //
  //    // Act & Assert
  //    assertThrows(
  //        StatusRuntimeException.class,
  //        () -> {
  //          orchestratorService.process(csvFolderPath).await().indefinitely();
  //        });
  //  }

  //  @Test
  //  public void testProcess_failure_processPaymentStatus(@TempDir Path tempDir) throws
  // URISyntaxException, IOException {
  //    // Arrange
  //    String csvFolderPath = "csv-folder";
  //    Path csvDir = tempDir.resolve(csvFolderPath);
  //    Files.createDirectories(csvDir);
  //    File testFile = csvDir.resolve("test.csv").toFile();
  //    testFile.createNewFile();
  //    URL folderUrl = csvDir.toUri().toURL();
  //
  //    Mockito.lenient().when(resourceLoader.getResource(csvFolderPath)).thenReturn(folderUrl);
  //
  //    InputCsvFileProcessingSvc.PaymentRecord paymentRecord =
  //        InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();
  //    PaymentsProcessingSvc.AckPaymentSent ackPaymentSent =
  //        PaymentsProcessingSvc.AckPaymentSent.newBuilder().build();
  //    PaymentsProcessingSvc.PaymentStatus paymentStatus =
  //        PaymentsProcessingSvc.PaymentStatus.newBuilder().build();
  //
  //    Mockito.lenient()
  //        .when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class)))
  //        .thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
  //    Mockito.lenient()
  //        .when(
  //            processCsvPaymentsInputFileService.remoteProcess(
  //                any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class)))
  //        .thenReturn(Multi.createFrom().item(paymentRecord));
  //    Mockito.lenient()
  //        .when(
  //            sendPaymentRecordService.remoteProcess(
  //                any(InputCsvFileProcessingSvc.PaymentRecord.class)))
  //        .thenReturn(Uni.createFrom().item(ackPaymentSent));
  //    Mockito.lenient()
  //        .when(
  //            processAckPaymentSentService.remoteProcess(
  //                any(PaymentsProcessingSvc.AckPaymentSent.class)))
  //        .thenReturn(Uni.createFrom().item(paymentStatus));
  //    Mockito.lenient()
  //        .when(
  //            processPaymentStatusService.remoteProcess(
  //                any(PaymentsProcessingSvc.PaymentStatus.class)))
  //        .thenReturn(Uni.createFrom().failure(new StatusRuntimeException(Status.INTERNAL)));
  //
  //    // Act & Assert
  //    assertThrows(
  //        StatusRuntimeException.class,
  //        () -> {
  //          orchestratorService.process(csvFolderPath).await().indefinitely();
  //        });
  //  }

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
  //        InputCsvFileProcessingSvc.PaymentRecord paymentRecord =
  // InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();
  //        PaymentsProcessingSvc.AckPaymentSent ackPaymentSent =
  // PaymentsProcessingSvc.AckPaymentSent.newBuilder().build();
  //        PaymentsProcessingSvc.PaymentStatus paymentStatus =
  // PaymentsProcessingSvc.PaymentStatus.newBuilder().build();
  //        PaymentStatusSvc.PaymentOutput paymentOutput =
  // PaymentStatusSvc.PaymentOutput.newBuilder().build();
  //        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile grpcOutputFile =
  // OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.newBuilder().build();
  //        CsvPaymentsOutputFile expectedOutputFile = new CsvPaymentsOutputFile(csvFolderPath +
  // "/test.csv");
  //
  //
  // Mockito.lenient().when(csvPaymentsInputFileMapper.toGrpc(any(CsvPaymentsInputFile.class))).thenReturn(InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().build());
  //
  // Mockito.lenient().when(processCsvPaymentsInputFileService.remoteProcess(any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class))).thenReturn(Multi.createFrom().item(paymentRecord));
  //
  // Mockito.lenient().when(sendPaymentRecordService.remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class)))
  //                .thenReturn(Uni.createFrom().failure(new
  // StatusRuntimeException(Status.RESOURCE_EXHAUSTED)))
  //                .thenReturn(Uni.createFrom().failure(new
  // StatusRuntimeException(Status.RESOURCE_EXHAUSTED)))
  //                .thenReturn(Uni.createFrom().item(ackPaymentSent));
  //
  // Mockito.lenient().when(processAckPaymentSentService.remoteProcess(any(PaymentsProcessingSvc.AckPaymentSent.class))).thenReturn(Uni.createFrom().item(paymentStatus));
  //
  // Mockito.lenient().when(processPaymentStatusService.remoteProcess(any(PaymentsProcessingSvc.PaymentStatus.class))).thenReturn(Uni.createFrom().item(paymentOutput));
  //
  // Mockito.lenient().when(processCsvPaymentsOutputFileService.remoteProcess(any(Multi.class))).thenReturn(Uni.createFrom().item(grpcOutputFile));
  //
  // Mockito.lenient().when(csvPaymentsOutputFileMapper.fromGrpc(any(OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.class))).thenReturn(expectedOutputFile);
  //
  //        // Act
  //        orchestratorService.process(csvFolderPath).await().indefinitely();
  //
  //        // Assert
  //        Mockito.verify(sendPaymentRecordService,
  // times(3)).remoteProcess(any(InputCsvFileProcessingSvc.PaymentRecord.class));
  //
  //        // Cleanup
  //        testFile.delete();
  //    }
}
