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

package io.github.mbarcia.pipeline.step.collection;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.PipelineRunner;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CollectionStepsTest {

  PipelineRunner pipelineRunner = new PipelineRunner();

  @Test
  void testOneToManyCollectionStep() {
    // Given: Create test data
    Multi<String> input = Multi.createFrom().items("Payment1", "Payment2");

    // Create steps
    ValidatePaymentStepBlocking validateStep = new ValidatePaymentStepBlocking();
    validateStep.liveConfig().overrides().autoPersist(false);

    ExpandPaymentCollectionStep expandStep = new ExpandPaymentCollectionStep();
    expandStep.liveConfig().overrides().autoPersist(false);

    // When: Run pipeline
    AssertSubscriber<String> subscriber =
        pipelineRunner
            .run(input, List.of(validateStep, expandStep))
            .onItem()
            .castTo(String.class)
            .subscribe()
            .withSubscriber(AssertSubscriber.create(6)); // 2 inputs * 3 expanded each

    // Then: Verify results
    subscriber.awaitItems(6).awaitCompletion(Duration.ofSeconds(10));

    List<String> results = subscriber.getItems();
    assertEquals(6, results.size());

    // Verify all items were processed
    assertTrue(results.contains("TXN-001-Validated: Payment1"));
    assertTrue(results.contains("TXN-002-Validated: Payment1"));
    assertTrue(results.contains("TXN-003-Validated: Payment1"));
    assertTrue(results.contains("TXN-001-Validated: Payment2"));
    assertTrue(results.contains("TXN-002-Validated: Payment2"));
    assertTrue(results.contains("TXN-003-Validated: Payment2"));
  }

  // Helper step for validating payments
  public static class ValidatePaymentStepBlocking extends ConfigurableStep
      implements StepOneToOneBlocking<String, String> {

    @Override
    public String apply(String payment) {
      // Simulate some processing time
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      return "Validated: " + payment;
    }
  }
}
