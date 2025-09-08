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

package io.github.mbarcia.csv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.common.domain.CsvPaymentsOutputFile;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.pipeline.service.ProcessAckPaymentStep;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OrchestratorServiceTest {

  @TempDir Path tempDir;

  @InjectMocks private OrchestratorService orchestratorService;

  @Mock private ProcessFolderStep processFolderStep;

  @Mock private ProcessInputFileStep processInputFileStep;

  @Mock private ProcessOutputFileStep processOutputFileStep;

  @Mock private PersistAndSendPaymentStep persistAndSendPaymentStep;

  @Mock private ProcessAckPaymentStep processAckPaymentStep;

  @Mock private ProcessPaymentStatusStep processPaymentStatusStep;

  @Mock private ProcessPipelineConfig config;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(config.getConcurrencyLimitRecords()).thenReturn(1000);
  }

  @Test
  void testProcess_MultipleFiles() throws URISyntaxException, IOException {
    // Given
    String folderPath = tempDir.toString();
    CsvPaymentsInputFile inputFile1 =
        new CsvPaymentsInputFile(new File(tempDir.toFile(), "test1.csv"));
    CsvPaymentsInputFile inputFile2 =
        new CsvPaymentsInputFile(new File(tempDir.toFile(), "test2.csv"));
    CsvPaymentsOutputFile outputFile1 = new CsvPaymentsOutputFile(inputFile1.getFilepath());
    CsvPaymentsOutputFile outputFile2 = new CsvPaymentsOutputFile(inputFile2.getFilepath());

    Stream<CsvPaymentsInputFile> inputFileStream = Stream.of(inputFile1, inputFile2);
    Multi<CsvPaymentsInputFile> inputFileMulti = Multi.createFrom().items(inputFileStream);
    Uni<Multi<CsvPaymentsInputFile>> inputFileUni = Uni.createFrom().item(inputFileMulti);

    when(processFolderStep.execute(folderPath)).thenReturn(inputFileUni);

    // Create mock Multi streams for payment records
    Multi<InputCsvFileProcessingSvc.PaymentRecord> emptyPaymentRecordsMulti =
        Multi.createFrom().empty();
    Uni<Multi<InputCsvFileProcessingSvc.PaymentRecord>> paymentRecordsUni1 =
        Uni.createFrom().item(emptyPaymentRecordsMulti);
    Uni<Multi<InputCsvFileProcessingSvc.PaymentRecord>> paymentRecordsUni2 =
        Uni.createFrom().item(emptyPaymentRecordsMulti);

    // Mock the processInputFileStep to return empty streams
    when(processInputFileStep.execute(any())).thenReturn(paymentRecordsUni1, paymentRecordsUni2);

    // Mock the processOutputFileStep to return completed Uni
    when(processOutputFileStep.execute(any())).thenReturn(Uni.createFrom().item(outputFile1));

    // When
    Uni<Void> resultUni = orchestratorService.process(folderPath);
    UniAssertSubscriber<Void> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitItem().assertCompleted();

    // Verify interactions
    verify(processFolderStep).execute(folderPath);
    verify(processInputFileStep, times(2)).execute(any());
    verify(processOutputFileStep, times(2)).execute(any());
  }

  @Test
  void testProcess_EmptyFolder() throws URISyntaxException, IOException {
    // Given
    String folderPath = tempDir.toString();
    Stream<CsvPaymentsInputFile> emptyInputStream = Stream.empty();
    Multi<CsvPaymentsInputFile> emptyInputMulti = Multi.createFrom().items(emptyInputStream);
    Uni<Multi<CsvPaymentsInputFile>> emptyInputUni = Uni.createFrom().item(emptyInputMulti);

    when(processFolderStep.execute(folderPath)).thenReturn(emptyInputUni);

    // When
    Uni<Void> resultUni = orchestratorService.process(folderPath);
    UniAssertSubscriber<Void> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitItem().assertCompleted();

    // Verify interactions
    verify(processFolderStep).execute(folderPath);
  }

  @Test
  void testProcess_FolderServiceThrowsException() throws URISyntaxException, IOException {
    // Given
    String folderPath = "error-folder";
    when(processFolderStep.execute(folderPath))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Processing failed")));

    // When
    Uni<Void> resultUni = orchestratorService.process(folderPath);
    UniAssertSubscriber<Void> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitFailure().assertFailedWith(RuntimeException.class);

    // Verify interactions
    verify(processFolderStep).execute(folderPath);
  }
}
