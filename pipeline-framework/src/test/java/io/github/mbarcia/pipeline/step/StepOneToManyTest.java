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
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.Test;

class StepOneToManyTest {

  static class TestStep implements StepOneToMany<String, String> {
    @Override
    public Multi<String> applyMulti(String input) {
      return Multi.createFrom().items(input + "1", input + "2", input + "3");
    }

    @Override
    public io.github.mbarcia.pipeline.config.StepConfig effectiveConfig() {
      return new io.github.mbarcia.pipeline.config.StepConfig();
    }
  }

  @Test
  void testApplyMultiMethod() {
    // Given
    TestStep step = new TestStep();

    // When
    Multi<String> result = step.applyMulti("test");

    // Then
    AssertSubscriber<String> subscriber = AssertSubscriber.create(3);
    result.subscribe().withSubscriber(subscriber);
    subscriber.awaitItems(3);

    assertEquals(3, subscriber.getItems().size());
    assertEquals("test1", subscriber.getItems().get(0));
    assertEquals("test2", subscriber.getItems().get(1));
    assertEquals("test3", subscriber.getItems().get(2));
  }
}
