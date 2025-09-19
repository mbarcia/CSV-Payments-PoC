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

package io.github.mbarcia.pipeline.step;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.config.StepConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class StepManyToOneTest {

  static class TestStep implements StepManyToOne<String, String> {
    @Override
    public Uni<String> applyBatch(List<String> inputs) {
      return Uni.createFrom().item("Batch processed: " + String.join(", ", inputs));
    }

    @Override
    public StepConfig effectiveConfig() {
      return new StepConfig();
    }

    @Override
    public int batchSize() {
      return 2; // Small batch size for testing
    }
  }

  @Test
  void testApplyBatchMethod() {
    // Given
    TestStep step = new TestStep();
    List<String> inputs = List.of("item1", "item2", "item3");

    // When
    Uni<String> result = step.applyBatch(inputs);

    // Then
    String value = result.await().indefinitely();
    assertEquals("Batch processed: item1, item2, item3", value);
  }

  @Test
  void testApplyMethod() {
    // Given
    TestStep step = new TestStep();
    Multi<Object> input = Multi.createFrom().items("item1", "item2", "item3", "item4");

    // When
    Multi<Object> result = step.apply(input);

    // Then
    AssertSubscriber<Object> subscriber =
        result.subscribe().withSubscriber(AssertSubscriber.create(2));
    subscriber.awaitItems(2, Duration.ofSeconds(5));
    subscriber.assertItems("Batch processed: item1, item2", "Batch processed: item3, item4");
  }
}
