/*
 * Copyright (c) 2023-2025 Mariano Barcia
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

package org.pipelineframework.csv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pipelineframework.csv.common.domain.CsvFolder;
import org.pipelineframework.csv.common.domain.CsvPaymentsInputFile;
import org.pipelineframework.csv.util.HybridResourceLoader;

public class ProcessFolderServiceTest {

    @Mock private HybridResourceLoader resourceLoader;

    @InjectMocks private ProcessFolderService processFolderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessWithValidDirectory(@TempDir Path tempDir) throws Exception {
        // Given
        CsvFolder csvFolder = new CsvFolder(tempDir);

        // Create CSV files in the temporary directory
        File file1 = new File(tempDir.toFile(), "file1.csv");
        File file2 = new File(tempDir.toFile(), "file2.csv");
        file1.createNewFile();
        file2.createNewFile();

        URL mockUrl = tempDir.toFile().toURI().toURL();

        when(resourceLoader.getResource(String.valueOf(tempDir))).thenReturn(mockUrl);

        // When
        Multi<CsvPaymentsInputFile> result = processFolderService.process(csvFolder);

        // Then
        AssertSubscriber<CsvPaymentsInputFile> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(2));
        subscriber.awaitCompletion();

        List<CsvPaymentsInputFile> resultList = subscriber.getItems();
        assertEquals(2, resultList.size());
    }

    @Test
    void testProcessWithNonExistentDirectory() {
        // Given
        CsvFolder csvFolder = new CsvFolder(Path.of("non-existent"));

        when(resourceLoader.getResource(String.valueOf(csvFolder.getPath()))).thenReturn(null);

        // When & Then
        Multi<CsvPaymentsInputFile> result = processFolderService.process(csvFolder);
        AssertSubscriber<CsvPaymentsInputFile> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.awaitFailure();
        assertTrue(subscriber.getFailure() instanceof IllegalArgumentException);
    }

    @Test
    @SneakyThrows
    void testProcessWithEmptyDirectory(@TempDir Path tempDir) {
        // Given
        String folderPath = "empty-folder";
        CsvFolder csvFolder = new CsvFolder(Path.of(folderPath));
        URL mockUrl = tempDir.toFile().toURI().toURL();

        when(resourceLoader.getResource(folderPath)).thenReturn(mockUrl);

        // When
        Multi<CsvPaymentsInputFile> result = processFolderService.process(csvFolder);

        // Then
        AssertSubscriber<CsvPaymentsInputFile> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.awaitCompletion();

        List<CsvPaymentsInputFile> resultList = subscriber.getItems();
        assertTrue(resultList.isEmpty());
    }

    @Test
    @SneakyThrows
    void testProcessWithNoCsvFiles(@TempDir Path tempDir) {
        // Given
        CsvFolder csvFolder = new CsvFolder(tempDir);

        // Create non-CSV files in the temporary directory
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.doc");
        file1.createNewFile();
        file2.createNewFile();

        URL mockUrl = tempDir.toFile().toURI().toURL();

        when(resourceLoader.getResource(String.valueOf(csvFolder.getPath()))).thenReturn(mockUrl);

        // When
        Multi<CsvPaymentsInputFile> result = processFolderService.process(csvFolder);

        // Then
        AssertSubscriber<CsvPaymentsInputFile> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create());
        subscriber.awaitCompletion();

        List<CsvPaymentsInputFile> resultList = subscriber.getItems();
        assertTrue(resultList.isEmpty());
    }
}
