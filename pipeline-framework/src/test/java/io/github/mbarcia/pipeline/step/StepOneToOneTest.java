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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class StepOneToOneTest {

  static class TestStep implements StepOneToOne<String, String> {
    @Override
    public Uni<String> applyAsyncUni(String input) {
      return Uni.createFrom().item("Processed: " + input);
    }

    @Override
    public io.github.mbarcia.pipeline.config.StepConfig effectiveConfig() {
      return new io.github.mbarcia.pipeline.config.StepConfig();
    }
  }

  @Test
  void testApplyAsyncUniMethod() {
    // Given
    TestStep step = new TestStep();

    // When
    Uni<String> result = step.applyAsyncUni("test");

    // Then
    String value = result.await().indefinitely();
    assertEquals("Processed: test", value);
  }

  @Test
  void testApplyMethod() {
    // Given
    TestStep step = new TestStep();
    Multi<Object> input = Multi.createFrom().items("item1", "item2");

    // When
    Multi<Object> result = step.apply(input);

    // Then
    AssertSubscriber<Object> subscriber =
        result.subscribe().withSubscriber(AssertSubscriber.create(2));
    subscriber.awaitItems(2, Duration.ofSeconds(5));
    subscriber.assertItems("Processed: item1", "Processed: item2");
  }
}
