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

import io.github.mbarcia.csv.CsvPaymentsApplication;
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.*;

@QuarkusTest
class CsvPaymentsApplicationTest {

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

  @Inject EntityManager entityManager;

  @Test
  void testAutoPersistenceOfPaymentRecord() {
    // Create a PaymentRecord entity
    PaymentRecord paymentRecord = new PaymentRecord();
    paymentRecord.setCsvId("1");
    paymentRecord.setRecipient("John Doe");
    paymentRecord.setAmount(new BigDecimal("100.00"));
    paymentRecord.setCurrency(Currency.getInstance("USD"));
    paymentRecord.setCsvPaymentsInputFilePath(Paths.get("/test/path/file.csv"));

    // Simulate the auto-persistence that should happen in the pipeline
    entityManager.persist(paymentRecord);
    entityManager.flush();

    // Verify the entity was persisted
    List<PaymentRecord> records =
        entityManager
            .createQuery(
                "SELECT p FROM PaymentRecord p WHERE p.recipient = :recipient", PaymentRecord.class)
            .setParameter("recipient", "John Doe")
            .getResultList();

    assertThat(records).hasSize(1);
    assertThat(records.get(0).getRecipient()).isEqualTo("John Doe");
    assertThat(records.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    assertThat(records.get(0).getCurrency()).isEqualTo(Currency.getInstance("USD"));
  }
}
