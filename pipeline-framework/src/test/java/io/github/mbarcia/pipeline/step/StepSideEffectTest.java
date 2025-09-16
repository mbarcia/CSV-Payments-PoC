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

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

class StepSideEffectTest {

  static class TestStep implements StepSideEffect<String> {
    @Override
    public Uni<Void> apply(String input) {
      // Simulate some side effect
      System.out.println("Side effect for: " + input);
      return Uni.createFrom().voidItem();
    }

    @Override
    public io.github.mbarcia.pipeline.config.StepConfig effectiveConfig() {
      return new io.github.mbarcia.pipeline.config.StepConfig();
    }
  }

  @Test
  void testApplyMethod() {
    // Given
    TestStep step = new TestStep();

    // When
    Uni<Void> result = step.apply("test");

    // Then
    Void value = result.await().indefinitely();
    assertNull(value);
  }

  @Test
  void testDefaultConcurrency() {
    // Given
    TestStep step = new TestStep();

    // When
    int concurrency = step.concurrency();

    // Then
    assertEquals(1, concurrency);
  }

  @Test
  void testDefaultRunWithVirtualThreads() {
    // Given
    TestStep step = new TestStep();

    // When
    boolean runWithVirtualThreads = step.runWithVirtualThreads();

    // Then
    assertFalse(runWithVirtualThreads);
  }
}
