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

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.util.HybridResourceLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProcessFolderService {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessFolderService.class);

  @Inject
  HybridResourceLoader resourceLoader;

  public Stream<CsvPaymentsInputFile> process(String csvFolderPath)
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
      return Stream.empty();
    }

    return Stream.of(csvFiles)
        .map(CsvPaymentsInputFile::new);
  }
}
