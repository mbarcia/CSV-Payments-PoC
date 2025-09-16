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
import java.util.List;
import org.junit.jupiter.api.Test;

class StepManyToManyTest {

  static class TestStep implements StepManyToMany {
    @Override
    public Multi<Object> applyStreaming(Multi<Object> upstream) {
      return upstream.onItem().transform(item -> "Processed: " + item.toString());
    }

    @Override
    public io.github.mbarcia.pipeline.config.StepConfig effectiveConfig() {
      return new io.github.mbarcia.pipeline.config.StepConfig();
    }
  }

  @Test
  void testApplyStreamingMethod() {
    // Given
    TestStep step = new TestStep();
    Multi<Object> input = Multi.createFrom().items("item1", "item2", "item3");

    // When
    Multi<Object> result = step.applyStreaming(input);

    // Then
    List<Object> items = result.collect().asList().await().indefinitely();

    assertEquals(3, items.size());
    assertEquals("Processed: item1", items.get(0));
    assertEquals("Processed: item2", items.get(1));
    assertEquals("Processed: item3", items.get(2));
  }

  @Test
  void testDeadLetterMultiDefaultImplementation() {
    // Given
    TestStep step = new TestStep();
    Multi<Object> upstream = Multi.createFrom().items("item1", "item2");

    // When
    Multi<?> result = step.deadLetterMulti(upstream, new RuntimeException("test error"));

    // Then
    List<?> items = result.collect().asList().await().indefinitely();
    assertEquals(0, items.size());
  }
}
