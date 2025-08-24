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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import io.quarkus.runtime.Quarkus;
import io.smallrye.mutiny.Uni;
import java.net.URISyntaxException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import picocli.CommandLine;

class CsvPaymentsApplicationTest {

  @InjectMocks CsvPaymentsApplication application;

  @Mock OrchestratorService orchestratorService;

  @Mock SystemExiter exiter;

  @Mock Sync sync;

  @Mock CommandLine.IFactory factory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    doNothing().when(exiter).exit(anyInt());
  }

  @Test
  void testRun_Success() throws URISyntaxException {
    // Arrange
    String csvFolder = "test-folder";
    application.csvFolder = csvFolder;
    when(orchestratorService.process(csvFolder)).thenReturn(Uni.createFrom().voidItem());

    // Act
    application.run();

    // Assert
    verify(orchestratorService).process(csvFolder);
    verify(exiter).exit(0);
  }

  @Test
  void testRun_ProcessingFailure() throws URISyntaxException {
    // Arrange
    String csvFolder = "test-folder";
    application.csvFolder = csvFolder;
    when(orchestratorService.process(csvFolder))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Processing failed")));

    // Act
    application.run();

    // Assert
    verify(orchestratorService).process(csvFolder);
    verify(exiter).exit(1);
  }

  @Test
  @SneakyThrows
  void testRun_InterruptedException() {
    // Arrange
    String csvFolder = "test-folder";
    application.csvFolder = csvFolder;
    when(orchestratorService.process(csvFolder)).thenReturn(Uni.createFrom().voidItem());
    doThrow(new InterruptedException("Simulated interrupt")).when(sync).await();

    // Act
    application.run();

    // Assert
    verify(orchestratorService).process(csvFolder);
    verify(exiter).exit(2);
  }

  @Test
  void testRun_InvalidFolder() throws URISyntaxException {
    // Arrange
    String csvFolder = "invalid-folder";
    application.csvFolder = csvFolder;
    when(orchestratorService.process(csvFolder))
        .thenThrow(new URISyntaxException(csvFolder, "Invalid folder"));

    // Act
    application.run();

    // Assert
    verify(orchestratorService).process(csvFolder);
    verify(exiter).exit(1);
  }

  @Test
  void testRun_Args() throws URISyntaxException {
    // Arrange
    String[] args = {"--csv-folder", "test-folder"};
    when(orchestratorService.process("test-folder")).thenReturn(Uni.createFrom().voidItem());

    // Act
    int exitCode = application.run(args);

    // Assert
    assertEquals(0, exitCode);
    verify(orchestratorService).process("test-folder");
    verify(exiter).exit(0);
  }

  @Test
  void testMain() {
    // Arrange
    String[] args = {"--csv-folder", "test-folder"};
    try (MockedStatic<Quarkus> quarkusMock = Mockito.mockStatic(Quarkus.class)) {
      // Act
      CsvPaymentsApplication.main(args);

      // Assert
      quarkusMock.verify(() -> Quarkus.run(CsvPaymentsApplication.class, args));
    }
  }
}
