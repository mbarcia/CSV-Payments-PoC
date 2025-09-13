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

package io.github.mbarcia.csv.step;

import io.github.mbarcia.csv.common.mapper.CsvPaymentsOutputFileMapper;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for ProcessOutputFileStep to ensure records from different input files are properly
 * grouped and processed separately.
 */
class ProcessOutputFileStepTest {

  @Mock
  MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub
      processCsvPaymentsOutputFileService;

  @Mock CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

  @Mock PipelineConfig pipelineConfig;

  ProcessOutputFileStep processOutputFileStep;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    processOutputFileStep = new ProcessOutputFileStep(pipelineConfig);

    // Inject mocks using reflection since the fields are private
    try {
      java.lang.reflect.Field serviceField =
          ProcessOutputFileStep.class.getDeclaredField("processCsvPaymentsOutputFileService");
      serviceField.setAccessible(true);
      serviceField.set(processOutputFileStep, processCsvPaymentsOutputFileService);

      java.lang.reflect.Field mapperField =
          ProcessOutputFileStep.class.getDeclaredField("csvPaymentsOutputFileMapper");
      mapperField.setAccessible(true);
      mapperField.set(processOutputFileStep, csvPaymentsOutputFileMapper);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mocks", e);
    }
  }

  @Test
  void testGetInputFilePath_shouldExtractCorrectPath() throws Exception {
    // Given
    PaymentStatusSvc.PaymentOutput paymentOutput = createPaymentOutputWithPath();

    // When
    String inputFilePath = getInputFilePathUsingReflection(paymentOutput);

    // Then
    org.junit.jupiter.api.Assertions.assertEquals("/path/to/first.csv", inputFilePath);
  }

  @Test
  void testGetInputFilePath_shouldHandleMissingPath() throws Exception {
    // Given
    PaymentStatusSvc.PaymentOutput paymentOutput = createPaymentOutputWithoutPath();

    // When
    String inputFilePath = getInputFilePathUsingReflection(paymentOutput);

    // Then
    org.junit.jupiter.api.Assertions.assertEquals("unknown", inputFilePath);
  }

  private PaymentStatusSvc.PaymentOutput createPaymentOutputWithPath() {
    // Create the nested structure that the real implementation expects
    InputCsvFileProcessingSvc.PaymentRecord paymentRecord =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
            .setCsvPaymentsInputFilePath("/path/to/first.csv")
            .build();

    PaymentsProcessingSvc.AckPaymentSent ackPaymentSent =
        PaymentsProcessingSvc.AckPaymentSent.newBuilder()
            .setPaymentRecord(paymentRecord)
            .setConversationId(UUID.randomUUID().toString())
            .build();

    PaymentsProcessingSvc.PaymentStatus paymentStatus =
        PaymentsProcessingSvc.PaymentStatus.newBuilder()
            .setAckPaymentSent(ackPaymentSent)
            .setStatus("SUCCESS")
            .build();

    return PaymentStatusSvc.PaymentOutput.newBuilder()
        .setPaymentStatus(paymentStatus)
        .setCsvId(UUID.randomUUID().toString())
        .setRecipient("test@example.com")
        .setAmount("100.00")
        .setCurrency("USD")
        .setConversationId(UUID.randomUUID().toString())
        .build();
  }

  private PaymentStatusSvc.PaymentOutput createPaymentOutputWithoutPath() {
    // Create a payment output without the nested structure
    PaymentsProcessingSvc.PaymentStatus paymentStatus =
        PaymentsProcessingSvc.PaymentStatus.newBuilder().setStatus("SUCCESS").build();

    return PaymentStatusSvc.PaymentOutput.newBuilder()
        .setPaymentStatus(paymentStatus)
        .setCsvId(UUID.randomUUID().toString())
        .setRecipient("test@example.com")
        .setAmount("100.00")
        .setCurrency("USD")
        .setConversationId(UUID.randomUUID().toString())
        .build();
  }

  private String getInputFilePathUsingReflection(PaymentStatusSvc.PaymentOutput paymentOutput)
      throws Exception {
    java.lang.reflect.Method method =
        ProcessOutputFileStep.class.getDeclaredMethod(
            "getInputFilePath", PaymentStatusSvc.PaymentOutput.class);
    method.setAccessible(true);
    return (String) method.invoke(processOutputFileStep, paymentOutput);
  }
}
