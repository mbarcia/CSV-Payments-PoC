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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessFileServiceTest {

  @TempDir Path tempDir;

  @InjectMocks private ProcessFileService processFileService;

  @Mock
  private MutinyProcessCsvPaymentsInputFileServiceGrpc.MutinyProcessCsvPaymentsInputFileServiceStub
      processCsvPaymentsInputFileService;

  @Mock
  MutinyPersistPaymentRecordServiceGrpc.MutinyPersistPaymentRecordServiceStub
      persistPaymentRecordService;

  @Mock
  private MutinySendPaymentRecordServiceGrpc.MutinySendPaymentRecordServiceStub
      sendPaymentRecordService;

  @Mock
  MutinyPersistAckPaymentSentServiceGrpc.MutinyPersistAckPaymentSentServiceStub
      persistAckPaymentSentService;

  @Mock
  private MutinyProcessAckPaymentSentServiceGrpc.MutinyProcessAckPaymentSentServiceStub
      processAckPaymentSentService;

  @Mock
  private MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub
      processPaymentStatusService;

  @Mock
  private MutinyProcessCsvPaymentsOutputFileServiceGrpc
          .MutinyProcessCsvPaymentsOutputFileServiceStub
      processCsvPaymentsOutputFileService;

  @Mock private CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

  @Mock private CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

  @Mock private ProcessFileServiceConfig config;

  // Test data
  private CsvPaymentsInputFile domainInputFile;
  private InputCsvFileProcessingSvc.CsvPaymentsInputFile grpcInputFile;
  private InputCsvFileProcessingSvc.PaymentRecord grpcPaymentRecord;
  private PaymentsProcessingSvc.AckPaymentSent grpcAckPaymentSent;
  private PaymentsProcessingSvc.PaymentStatus grpcPaymentStatus;
  private PaymentStatusSvc.PaymentOutput grpcPaymentOutput;
  private OutputCsvFileProcessingSvc.CsvPaymentsOutputFile grpcOutputFile;
  private CsvPaymentsOutputFile domainOutputFile;

  @BeforeEach
  void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);
    // Domain objects
    domainInputFile = new CsvPaymentsInputFile(new File(tempDir.toFile(), "test.csv"));
    domainOutputFile = new CsvPaymentsOutputFile(domainInputFile.getFilepath());

    // gRPC objects
    grpcInputFile =
        InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder().setFilepath("test.csv").build();
    grpcPaymentRecord =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder().setId("record1").build();
    grpcAckPaymentSent = PaymentsProcessingSvc.AckPaymentSent.newBuilder().setId("ack1").build();
    grpcPaymentStatus = PaymentsProcessingSvc.PaymentStatus.newBuilder().setId("status1").build();
    grpcPaymentOutput = PaymentStatusSvc.PaymentOutput.newBuilder().setId("output1").build();
    grpcOutputFile =
        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.newBuilder()
            .setFilepath("test.csv.out")
            .build();

    // Configure mock values
    when(config.getConcurrencyLimitRecords()).thenReturn(1000);
    when(config.getMaxRetries()).thenReturn(3);
    when(config.getInitialRetryDelay()).thenReturn(100L);
  }

  @Test
  void testProcess_Success() {
    // Given
    when(csvPaymentsInputFileMapper.toGrpc(domainInputFile)).thenReturn(grpcInputFile);
    when(processCsvPaymentsInputFileService.remoteProcess(grpcInputFile))
        .thenReturn(Multi.createFrom().item(grpcPaymentRecord));
    when(persistPaymentRecordService.remoteProcess(grpcPaymentRecord))
        .thenReturn(Uni.createFrom().item(grpcPaymentRecord));
    when(sendPaymentRecordService.remoteProcess(grpcPaymentRecord))
        .thenReturn(Uni.createFrom().item(grpcAckPaymentSent));
    when(persistAckPaymentSentService.remoteProcess(grpcAckPaymentSent))
        .thenReturn(Uni.createFrom().item(grpcAckPaymentSent));
    when(processAckPaymentSentService.remoteProcess(grpcAckPaymentSent))
        .thenReturn(Uni.createFrom().item(grpcPaymentStatus));
    when(processPaymentStatusService.remoteProcess(grpcPaymentStatus))
        .thenReturn(Uni.createFrom().item(grpcPaymentOutput));

    // Argument captor for the stream argument
    ArgumentCaptor<Multi<PaymentStatusSvc.PaymentOutput>> captor =
        ArgumentCaptor.forClass(Multi.class);
    when(processCsvPaymentsOutputFileService.remoteProcess(captor.capture()))
        .thenAnswer(
            invocation -> {
              Multi<PaymentStatusSvc.PaymentOutput> multi = invocation.getArgument(0);
              return multi
                  .collect()
                  .asList()
                  .map(
                      _ -> {
                        // You can add assertions on the list here if needed
                        return grpcOutputFile;
                      });
            });

    when(csvPaymentsOutputFileMapper.fromGrpc(grpcOutputFile)).thenReturn(domainOutputFile);

    // When
    Uni<CsvPaymentsOutputFile> resultUni = processFileService.process(domainInputFile);
    UniAssertSubscriber<CsvPaymentsOutputFile> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitItem().assertItem(domainOutputFile);

    // Verify interactions
    verify(csvPaymentsInputFileMapper).toGrpc(domainInputFile);
    verify(processCsvPaymentsInputFileService).remoteProcess(grpcInputFile);
    verify(persistPaymentRecordService).remoteProcess(grpcPaymentRecord);
    verify(sendPaymentRecordService).remoteProcess(grpcPaymentRecord);
    verify(persistAckPaymentSentService).remoteProcess(grpcAckPaymentSent);
    verify(processAckPaymentSentService).remoteProcess(grpcAckPaymentSent);
    verify(processPaymentStatusService).remoteProcess(grpcPaymentStatus);
    verify(processCsvPaymentsOutputFileService).remoteProcess(any(Multi.class));
    verify(csvPaymentsOutputFileMapper).fromGrpc(grpcOutputFile);

    // To verify the content of the stream, we need to subscribe to it.
    // The captor captures the Multi, but the processing happens when subscribed.
    // The thenAnswer block above is a good way to simulate the subscription and
    // return a Uni.
  }

  @Test
  void testProcess_RetryOnThrottlingError() {
    // Given
    StatusRuntimeException throttlingError = new StatusRuntimeException(Status.RESOURCE_EXHAUSTED);

    when(csvPaymentsInputFileMapper.toGrpc(domainInputFile)).thenReturn(grpcInputFile);
    when(processCsvPaymentsInputFileService.remoteProcess(grpcInputFile))
        .thenReturn(Multi.createFrom().item(grpcPaymentRecord));
    when(persistPaymentRecordService.remoteProcess(grpcPaymentRecord))
        .thenReturn(Uni.createFrom().item(grpcPaymentRecord));

    when(sendPaymentRecordService.remoteProcess(grpcPaymentRecord))
        // Fail twice, then succeed
        .thenReturn(Uni.createFrom().failure(throttlingError))
        .thenReturn(Uni.createFrom().failure(throttlingError))
        .thenReturn(Uni.createFrom().item(grpcAckPaymentSent));

    when(persistAckPaymentSentService.remoteProcess(grpcAckPaymentSent))
        .thenReturn(Uni.createFrom().item(grpcAckPaymentSent));
    when(processAckPaymentSentService.remoteProcess(grpcAckPaymentSent))
        .thenReturn(Uni.createFrom().item(grpcPaymentStatus));
    when(processPaymentStatusService.remoteProcess(grpcPaymentStatus))
        .thenReturn(Uni.createFrom().item(grpcPaymentOutput));
    when(processCsvPaymentsOutputFileService.remoteProcess(any(Multi.class)))
        .thenAnswer(
            invocation -> {
              Multi<PaymentStatusSvc.PaymentOutput> multi = invocation.getArgument(0);
              return multi.collect().asList().map(_ -> grpcOutputFile);
            });
    when(csvPaymentsOutputFileMapper.fromGrpc(grpcOutputFile)).thenReturn(domainOutputFile);

    // When
    Uni<CsvPaymentsOutputFile> resultUni = processFileService.process(domainInputFile);
    UniAssertSubscriber<CsvPaymentsOutputFile> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitItem().assertItem(domainOutputFile);

    // Verify sendPaymentRecordService was called 3 times (1 initial + 2 retries)
    verify(sendPaymentRecordService, times(3)).remoteProcess(grpcPaymentRecord);
  }

  @Test
  void testProcess_PermanentFailureAfterRetries() {
    // Given
    StatusRuntimeException throttlingError = new StatusRuntimeException(Status.RESOURCE_EXHAUSTED);

    when(csvPaymentsInputFileMapper.toGrpc(domainInputFile)).thenReturn(grpcInputFile);
    when(processCsvPaymentsInputFileService.remoteProcess(grpcInputFile))
        .thenReturn(Multi.createFrom().item(grpcPaymentRecord));
    when(persistPaymentRecordService.remoteProcess(grpcPaymentRecord))
        .thenReturn(Uni.createFrom().item(grpcPaymentRecord));

    // Fail more than max retries
    when(sendPaymentRecordService.remoteProcess(grpcPaymentRecord))
        .thenReturn(Uni.createFrom().failure(throttlingError));

    // This mock is crucial to subscribe to the upstream multi and propagate its failure
    when(processCsvPaymentsOutputFileService.remoteProcess(any(Multi.class)))
        .thenAnswer(
            invocation -> {
              Multi<PaymentStatusSvc.PaymentOutput> multi = invocation.getArgument(0);
              // This will propagate the failure from the multi
              return multi.collect().asList().map(_ -> grpcOutputFile);
            });

    // When
    Uni<CsvPaymentsOutputFile> resultUni = processFileService.process(domainInputFile);
    UniAssertSubscriber<CsvPaymentsOutputFile> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitFailure().assertFailedWith(StatusRuntimeException.class);

    // Verify sendPaymentRecordService was called 4 times (1 initial + 3 retries)
    verify(sendPaymentRecordService, times(4)).remoteProcess(grpcPaymentRecord);
  }

  @Test
  void testIsThrottlingError() {
    assertTrue(
        processFileService.isThrottlingError(
            new StatusRuntimeException(Status.RESOURCE_EXHAUSTED)));
    assertTrue(
        processFileService.isThrottlingError(new StatusRuntimeException(Status.UNAVAILABLE)));
    assertTrue(processFileService.isThrottlingError(new StatusRuntimeException(Status.ABORTED)));
    assertFalse(
        processFileService.isThrottlingError(new StatusRuntimeException(Status.INVALID_ARGUMENT)));
    assertFalse(processFileService.isThrottlingError(new RuntimeException("Some other error")));
  }
}
