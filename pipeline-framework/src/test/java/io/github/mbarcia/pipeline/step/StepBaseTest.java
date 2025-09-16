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
import static org.mockito.Mockito.*;

import io.github.mbarcia.pipeline.config.StepConfig;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

class StepBaseTest {

  static class TestStep implements StepBase {
    private final StepConfig config;

    TestStep(StepConfig config) {
      this.config = config;
    }

    @Override
    public StepConfig effectiveConfig() {
      return config;
    }
  }

  @Test
  void testDefaultMethodsDelegateToConfig() {
    // Given
    StepConfig config = mock(StepConfig.class);
    when(config.retryLimit()).thenReturn(3);
    when(config.retryWait()).thenReturn(java.time.Duration.ofMillis(200));
    when(config.debug()).thenReturn(false);
    when(config.recoverOnFailure()).thenReturn(false);
    when(config.runWithVirtualThreads()).thenReturn(false);
    when(config.maxBackoff()).thenReturn(java.time.Duration.ofSeconds(30));
    when(config.jitter()).thenReturn(false);

    TestStep step = new TestStep(config);

    // When & Then
    assertEquals(3, step.retryLimit());
    assertEquals(java.time.Duration.ofMillis(200), step.retryWait());
    assertFalse(step.debug());
    assertFalse(step.recoverOnFailure());
    assertFalse(step.runWithVirtualThreads());
    assertEquals(java.time.Duration.ofSeconds(30), step.maxBackoff());
    assertFalse(step.jitter());
  }

  @Test
  void testDeadLetterDefaultImplementation() {
    // Given
    StepConfig config = new StepConfig();
    TestStep step = new TestStep(config);

    // When
    Uni<Void> result = step.deadLetter("testItem", new RuntimeException("test error"));

    // Then
    Void value = result.await().indefinitely();
    assertNull(value);
  }
}
