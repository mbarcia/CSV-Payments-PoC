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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OrchestratorServiceTest {

  @TempDir Path tempDir;

  @InjectMocks private OrchestratorService orchestratorService;

  @Mock private ProcessFileService processFileService;

  @Mock private ProcessFolderService processFolderService;

  private CsvPaymentsInputFile inputFile1;
  private CsvPaymentsInputFile inputFile2;
  private CsvPaymentsOutputFile outputFile1;
  private CsvPaymentsOutputFile outputFile2;

  @BeforeEach
  void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);
    inputFile1 = new CsvPaymentsInputFile(new File(tempDir.toFile(), "test1.csv"));
    inputFile2 = new CsvPaymentsInputFile(new File(tempDir.toFile(), "test2.csv"));
    outputFile1 = new CsvPaymentsOutputFile(inputFile1.getFilepath());
    outputFile2 = new CsvPaymentsOutputFile(inputFile2.getFilepath());
  }

  @Test
  void testProcess_Success() throws URISyntaxException {
    // Given
    String folderPath = "test-folder";
    Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> fileMap = new HashMap<>();
    fileMap.put(inputFile1, outputFile1);
    fileMap.put(inputFile2, outputFile2);

    when(processFolderService.process(folderPath)).thenReturn(fileMap);
    when(processFileService.process(inputFile1)).thenReturn(Uni.createFrom().item(outputFile1));
    when(processFileService.process(inputFile2)).thenReturn(Uni.createFrom().item(outputFile2));

    // When
    Uni<Void> resultUni = orchestratorService.process(folderPath);
    UniAssertSubscriber<Void> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitItem().assertCompleted();
    verify(processFolderService).process(folderPath);
    verify(processFileService).process(inputFile1);
    verify(processFileService).process(inputFile2);
  }

  @Test
  void testProcess_EmptyFolder() throws URISyntaxException {
    // Given
    String folderPath = "empty-folder";
    when(processFolderService.process(folderPath)).thenReturn(Collections.emptyMap());

    // When
    Uni<Void> resultUni = orchestratorService.process(folderPath);
    UniAssertSubscriber<Void> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitItem().assertCompleted();
    verify(processFolderService).process(folderPath);
    verify(processFileService, never()).process(any());
  }

  @Test
  void testProcess_FolderServiceThrowsException() throws URISyntaxException {
    // Given
    String folderPath = "error-folder";
    when(processFolderService.process(folderPath))
        .thenThrow(new URISyntaxException("input", "reason"));

    // When & Then
    assertThrows(URISyntaxException.class, () -> orchestratorService.process(folderPath));
    verify(processFolderService).process(folderPath);
    verify(processFileService, never()).process(any());
  }

  @Test
  void testProcess_FileServiceThrowsException() throws URISyntaxException {
    // Given
    String folderPath = "test-folder";
    RuntimeException exception = new RuntimeException("File processing failed");
    Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> fileMap = new HashMap<>();
    fileMap.put(inputFile1, outputFile1);

    when(processFolderService.process(folderPath)).thenReturn(fileMap);
    when(processFileService.process(inputFile1)).thenReturn(Uni.createFrom().failure(exception));

    // When
    Uni<Void> resultUni = orchestratorService.process(folderPath);
    UniAssertSubscriber<Void> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());

    // Then
    subscriber.awaitFailure().assertFailedWith(RuntimeException.class, "File processing failed");
    verify(processFolderService).process(folderPath);
    verify(processFileService).process(inputFile1);
  }
}
