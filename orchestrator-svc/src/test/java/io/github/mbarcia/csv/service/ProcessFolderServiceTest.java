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
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProcessFolderServiceTest {

  @Mock private HybridResourceLoader resourceLoader;

  @InjectMocks private ProcessFolderService processFolderService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testProcessWithValidDirectory(@TempDir Path tempDir) throws URISyntaxException, IOException {
    // Given
    String csvFolderPath = "test-csv-folder";

    // Create CSV files in the temporary directory
    File file1 = new File(tempDir.toFile(), "file1.csv");
    File file2 = new File(tempDir.toFile(), "file2.csv");
    assert file1.createNewFile();
    assert file2.createNewFile();

    URL mockUrl = tempDir.toFile().toURI().toURL();

    when(resourceLoader.getResource(csvFolderPath)).thenReturn(mockUrl);

    // When
    Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result =
        processFolderService.process(csvFolderPath);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  void testProcessWithNonExistentDirectory() {
    // Given
    String csvFolderPath = "non-existent-folder";

    when(resourceLoader.getResource(csvFolderPath)).thenReturn(null);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> processFolderService.process(csvFolderPath));
  }

  @Test
  @SneakyThrows
  void testProcessWithEmptyDirectory(@TempDir Path tempDir) {
    // Given
    String csvFolderPath = "empty-folder";
    URL mockUrl = tempDir.toFile().toURI().toURL();

    when(resourceLoader.getResource(csvFolderPath)).thenReturn(mockUrl);

    // When
    Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result =
        processFolderService.process(csvFolderPath);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @SneakyThrows
  void testProcessWithNoCsvFiles(@TempDir Path tempDir) {
    // Given
    String csvFolderPath = "no-csv-folder";

    // Create non-CSV files in the temporary directory
    File file1 = new File(tempDir.toFile(), "file1.txt");
    File file2 = new File(tempDir.toFile(), "file2.doc");
    assert file1.createNewFile();
    assert file2.createNewFile();

    URL mockUrl = tempDir.toFile().toURI().toURL();

    when(resourceLoader.getResource(csvFolderPath)).thenReturn(mockUrl);

    // When
    Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result =
        processFolderService.process(csvFolderPath);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @SneakyThrows
  void testProcessWithInvalidDirectoryPath(@TempDir Path tempDir) {
    // Given
    String falseFolderPath = "invalid-directory";

    // Create a file instead of directory
    File falseFolder = new File(tempDir.toFile(), falseFolderPath);
    assert falseFolder.createNewFile();

    URL mockUrl = falseFolder.toURI().toURL();
    when(resourceLoader.getResource(falseFolder.getPath())).thenReturn(mockUrl);

    // When & Then
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> processFolderService.process(falseFolder.getPath()));
    assertTrue(exception.getMessage().contains("CSV path is not a valid directory"));
  }

  @Test
  @SneakyThrows
  void testProcessWithInvalidOutputFile(@TempDir Path tempDir) {
    // Given
    String csvFolderPath = "normal-temp-dir";

    // Create a file instead of directory
    File readOnlyFile = new File(tempDir.toFile(), "read-only-file.csv");
    assert readOnlyFile.createNewFile();
    File readOnlyOutputFile = new File(tempDir.toFile(), "read-only-file.csv.out");
    assert readOnlyOutputFile.createNewFile();
    assert readOnlyOutputFile.setWritable(false);

    URL mockUrl = tempDir.toFile().toURI().toURL();
    when(resourceLoader.getResource(csvFolderPath)).thenReturn(mockUrl);

    // When & Then
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> processFolderService.process(csvFolderPath));
    assertEquals(
        exception.getMessage(),
        "Failed to setup output file for %s".formatted(readOnlyFile.getAbsolutePath()));
  }
}
