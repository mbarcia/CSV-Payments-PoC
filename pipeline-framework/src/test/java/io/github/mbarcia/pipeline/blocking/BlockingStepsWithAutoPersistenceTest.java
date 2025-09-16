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

package io.github.mbarcia.pipeline.blocking;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.PipelineRunner;
import io.github.mbarcia.pipeline.persistence.PersistenceService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BlockingStepsWithAutoPersistenceTest {

  private PipelineRunner pipelineRunner;
  private PersistenceService persistenceService;

  @BeforeEach
  void setUp() {
    // Create a mock persistence service for testing
    persistenceService =
        new PersistenceService() {
          @Override
          public <T> Uni<Void> persist(T entity) {
            // Mock implementation that just completes successfully
            return Uni.createFrom().voidItem();
          }

          @Override
          public <T> boolean supports(Class<T> entityType) {
            // Mock implementation that supports TestPaymentEntity
            return TestPaymentEntity.class.equals(entityType);
          }
        };

    // Create pipeline runner with mock persistence service
    pipelineRunner = new PipelineRunner();
  }

  @Test
  void testBlockingStepsWorkWithAutoPersistence() {
    // Given: Create test entities
    TestPaymentEntity payment1 = new TestPaymentEntity("John Doe", new BigDecimal("100.00"));
    TestPaymentEntity payment2 = new TestPaymentEntity("Jane Smith", new BigDecimal("250.50"));
    TestPaymentEntity payment3 =
        new TestPaymentEntity("Bob Johnson", new BigDecimal("-50.00")); // Invalid amount

    Multi<TestPaymentEntity> input = Multi.createFrom().items(payment1, payment2, payment3);

    // Create blocking steps
    ValidatePaymentStep validateStep = new ValidatePaymentStep();
    validateStep.liveConfig().overrides().autoPersist(true); // Enable auto-persistence

    EnrichPaymentStep enrichStep = new EnrichPaymentStep();
    enrichStep.liveConfig().overrides().autoPersist(true); // Enable auto-persistence

    // When: Run pipeline with imperative steps
    AssertSubscriber<TestPaymentEntity> subscriber =
        pipelineRunner
            .run(input, List.of(validateStep, enrichStep))
            .onItem()
            .castTo(TestPaymentEntity.class)
            .subscribe()
            .withSubscriber(AssertSubscriber.create(3));

    // Then: Verify results
    subscriber.awaitItems(3).awaitCompletion(Duration.ofSeconds(10));

    List<TestPaymentEntity> results = subscriber.getItems();
    assertEquals(3, results.size());

    // Verify first payment was processed correctly
    TestPaymentEntity result1 = results.get(0);
    assertEquals("John Doe", result1.getRecipient());
    assertEquals(new BigDecimal("100.00"), result1.getAmount());
    assertEquals("ENRICHED", result1.getStatus()); // Should be validated then enriched

    // Verify second payment was processed correctly
    TestPaymentEntity result2 = results.get(1);
    assertEquals("Jane Smith", result2.getRecipient());
    assertEquals(new BigDecimal("250.50"), result2.getAmount());
    assertEquals("ENRICHED", result2.getStatus());

    // Verify third payment was rejected (negative amount)
    TestPaymentEntity result3 = results.get(2);
    assertEquals("Bob Johnson", result3.getRecipient());
    assertEquals(new BigDecimal("-50.00"), result3.getAmount());
    assertEquals("REJECTED", result3.getStatus()); // Should be rejected due to negative amount

    // Verify persistence service supports our entity type
    assertTrue(persistenceService.supports(TestPaymentEntity.class));
  }

  @Test
  void testBlockingStepsWithVirtualThreads() {
    // Given: Create test entities
    TestPaymentEntity payment = new TestPaymentEntity("Test User", new BigDecimal("75.00"));

    Multi<TestPaymentEntity> input = Multi.createFrom().items(payment);

    // Create blocking step with virtual threads enabled
    ValidatePaymentStep validateStep = new ValidatePaymentStep();
    validateStep
        .liveConfig()
        .overrides()
        .autoPersist(true)
        .runWithVirtualThreads(true); // Enable virtual threads

    // When: Run pipeline
    AssertSubscriber<TestPaymentEntity> subscriber =
        pipelineRunner
            .run(input, List.of(validateStep))
            .onItem()
            .castTo(TestPaymentEntity.class)
            .subscribe()
            .withSubscriber(AssertSubscriber.create(1));

    // Then: Verify results
    subscriber.awaitItems(1).awaitCompletion(Duration.ofSeconds(5));

    List<TestPaymentEntity> results = subscriber.getItems();
    assertEquals(1, results.size());
    assertEquals("VALIDATED", results.get(0).getStatus());
  }
}
