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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.mbarcia.csv.common.domain.CsvFolder;
import io.github.mbarcia.csv.repository.CsvFolderRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class PersistCsvFolderReactiveServiceTest {

  @Mock private CsvFolderRepository repository;

  private PersistCsvFolderReactiveService persistCsvFolderReactiveService;
  private CsvFolder testFolder;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    persistCsvFolderReactiveService = new PersistCsvFolderReactiveService(repository);
    Path testPath = Paths.get("/test/folder");
    testFolder = new CsvFolder(testPath);

    // Mock the repository to return the same object
    when(repository.persist(any(CsvFolder.class))).thenReturn(Uni.createFrom().item(testFolder));
  }

  @Test
  void testProcess() {
    // Mock static methods
    try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
      Logger mockLogger = mock(Logger.class);
      try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
        loggerFactoryMock
            .when(() -> LoggerFactory.getLogger(any(Class.class)))
            .thenReturn(mockLogger);

        // Execute the method under test
        Uni<CsvFolder> result = persistCsvFolderReactiveService.process(testFolder);

        // Verify the result
        UniAssertSubscriber<CsvFolder> subscriber =
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem(testFolder);

        // Verify repository interaction
        verify(repository).persist(testFolder);

        // Verify MDC interactions
        mdcMock.verify(
            () -> MDC.put("serviceId", PersistCsvFolderReactiveService.class.toString()));
        mdcMock.verify(MDC::clear);

        // Verify logger interaction
        verify(mockLogger)
            .info(
                "Persisted entity {} for service {}",
                testFolder,
                PersistCsvFolderReactiveService.class.toString());
      }
    }
  }

  @Test
  void testGetRepository() {
    // Test that getRepository returns the correct repository
    PanacheRepository<CsvFolder> repo = persistCsvFolderReactiveService.getRepository();
    // We can't directly compare the wrapped repository, so we just check it's not null
    assertThat(repo).isNotNull();
  }

  @Test
  void testConstructorWithRepository() {
    // Test that the constructor with repository works
    PersistCsvFolderReactiveService service = new PersistCsvFolderReactiveService(repository);
    assertThat(service).isNotNull();
  }
}
