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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class CsvPaymentsApplicationTest {

  CsvPaymentsApplication application;

  @InjectMock
  OrchestratorService orchestratorService;

  @InjectMock
  SystemExiter exiter;

  @BeforeEach
  void setUp() {
    application = new CsvPaymentsApplication();
    application.orchestratorService = orchestratorService;
    application.exiter = exiter;

    Mockito.reset(orchestratorService, exiter);
    // Mock the exiter to prevent System.exit during tests
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
}
