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

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
class CsvPaymentsIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(CsvPaymentsIntegrationTest.class);

  private static final String TEST_RESOURCES_DIR = "src/test/resources";
  private static final String TEST_OUTPUT_DIR = "target/test-output";

  @Test
  void testCsvFileContent() throws Exception {
    // Verify that our test CSV file has the expected content
    Path testCsv = Paths.get(TEST_RESOURCES_DIR, "test-payments.csv");
    assertTrue(Files.exists(testCsv), "Test CSV file should exist");

    String content = Files.readString(testCsv);
    assertTrue(content.contains("Natasha Thomas"), "CSV should contain test data");
    assertTrue(content.contains("Marc Joyce"), "CSV should contain test data");
    assertTrue(content.contains("Anthony Franco"), "CSV should contain test data");

    LOG.info("CSV file content verified successfully");
  }
}
