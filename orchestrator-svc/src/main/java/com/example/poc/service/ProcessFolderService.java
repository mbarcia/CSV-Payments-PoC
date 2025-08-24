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

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProcessFolderService {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessFolderService.class);

  @Inject HybridResourceLoader resourceLoader;

  public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> process(String csvFolderPath)
      throws URISyntaxException {
    LOG.info("Reading CSV folder from path: {}", csvFolderPath);

    URL resource = resourceLoader.getResource(csvFolderPath);
    if (resource == null) {
      throw new IllegalArgumentException(
          MessageFormat.format("CSV folder not found: {0}", csvFolderPath));
    }

    File directory = new File(resource.toURI());
    if (!directory.exists() || !directory.isDirectory()) {
      throw new IllegalArgumentException(
          MessageFormat.format(
              "CSV path is not a valid directory: {0}", directory.getAbsolutePath()));
    }

    File[] csvFiles = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".csv"));
    if (csvFiles == null || csvFiles.length == 0) {
      LOG.warn("No CSV files found in {}", csvFolderPath);
      resourceLoader.diagnoseResourceAccess(csvFolderPath);
      return Collections.emptyMap();
    }

    Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = new HashMap<>();
    for (File file : csvFiles) {
      CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(file);
      try {
        CsvPaymentsOutputFile outputFile = new CsvPaymentsOutputFile(inputFile.getFilepath());
        result.put(inputFile, outputFile);
      } catch (IOException e) {
        LOG.warn("Failed to setup output file for: {}", file.getAbsolutePath(), e);
        throw new RuntimeException(MessageFormat.format("Failed to setup output file for {0}", file.getAbsolutePath()), e);
      }
    }

    return result;
  }
}
