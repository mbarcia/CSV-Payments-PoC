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

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;

class ProcessCsvPaymentsInputReactiveServiceTest {

  @TempDir Path tempDir;

  private Path tempCsvFile;

  private ProcessCsvPaymentsInputReactiveService service;

  @BeforeEach
  void setUp() throws IOException {
    tempCsvFile = tempDir.resolve("test.csv");
    String csvContent =
        "ID,Recipient,Amount,Currency\n"
            + UUID.randomUUID()
            + ",John Doe,100.00,USD\n"
            + UUID.randomUUID()
            + ",Jane Smith,200.50,EUR\n";
    Files.writeString(tempCsvFile, csvContent);
    MockitoAnnotations.openMocks(this);
    service = new ProcessCsvPaymentsInputReactiveService(Runnable::run);
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempCsvFile);
  }

  @Test
  void process() {
    // Given
    CsvPaymentsInputFile csvFile = new CsvPaymentsInputFile(tempCsvFile.toFile());

    // When
    Multi<PaymentRecord> resultMulti = service.process(csvFile);

    // Then
    AssertSubscriber<PaymentRecord> subscriber =
        resultMulti.subscribe().withSubscriber(AssertSubscriber.create(2));
    subscriber.awaitCompletion();

    List<PaymentRecord> records = subscriber.getItems();
    assertEquals(2, records.size());

    PaymentRecord record1 = records.getFirst();
    assertNotNull(record1.getCsvId());
    assertEquals("John Doe", record1.getRecipient());
    assertEquals(new BigDecimal("100.00"), record1.getAmount());
    assertEquals(Currency.getInstance("USD"), record1.getCurrency());
    assertEquals(csvFile.getFilepath(), record1.getCsvPaymentsInputFilePath());

    PaymentRecord record2 = records.get(1);
    assertNotNull(record2.getCsvId());
    assertEquals("Jane Smith", record2.getRecipient());
    assertEquals(new BigDecimal("200.50"), record2.getAmount());
    assertEquals(Currency.getInstance("EUR"), record2.getCurrency());
    assertEquals(csvFile.getFilepath(), record2.getCsvPaymentsInputFilePath());
  }

  @Test
  @SneakyThrows
  void process_fileNotFound() {
    try (CsvPaymentsInputFile csvFile =
        new CsvPaymentsInputFile(tempDir.resolve("nonexistent.csv").toFile())) {
      Multi<PaymentRecord> result = service.process(csvFile);

      // Subscribe to trigger the lazy processing
      AssertSubscriber<PaymentRecord> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(1));

      subscriber.awaitFailure();
      subscriber.assertFailedWith(RuntimeException.class);
    }
  }

  @Test
  void process_invalidCsvContent() throws IOException {
    // Given
    String invalidCsvContent =
        "ID,Recipient,Amount,Currency\n" + UUID.randomUUID() + ",John Doe,invalid_amount,USD\n";
    Files.writeString(tempCsvFile, invalidCsvContent);
    CsvPaymentsInputFile csvFile = new CsvPaymentsInputFile(tempCsvFile.toFile());

    // When
    Multi<PaymentRecord> resultMulti = service.process(csvFile);

    // Then
    AssertSubscriber<PaymentRecord> subscriber =
        resultMulti.subscribe().withSubscriber(AssertSubscriber.create(1));
    subscriber.awaitFailure();
    subscriber.assertFailedWith(RuntimeException.class);
  }
}
