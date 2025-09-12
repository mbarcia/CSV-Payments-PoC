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

package io.github.mbarcia.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.step.StepBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PipelineRunnerTest {

  @Inject PipelineConfig pipelineConfig;

  @Test
  void testPipelineRunnerCreation() {
    try (PipelineRunner runner = new PipelineRunner()) {
      assertNotNull(runner);
    } catch (Exception e) {
      fail("PipelineRunner should be AutoCloseable");
    }
  }

  @Test
  void testRunWithStepOneToOne() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1", "item2", "item3");
      List<StepBase> steps = List.of(new TestSteps.TestStepOneToOne());

      Multi<Object> result = runner.run(input, steps);

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(3));
      subscriber.awaitItems(3, Duration.ofSeconds(5)).assertCompleted();

      // Grab all items
      List<Object> actualItems = subscriber.getItems();

      // Expected items
      Set<String> expectedItems =
          Set.of("Processed: item1", "Processed: item2", "Processed: item3");

      // Assert ignoring order
      assertEquals(expectedItems, new HashSet<>(actualItems));

      // If duplicates matter, use sorted list instead:
      // List<String> expectedList = List.of(
      //    "Processed: item1",
      //    "Processed: item2",
      //    "Processed: item3"
      // );
      // assertEquals(expectedList.stream().sorted().toList(),
      //              actualItems.stream().map(Object::toString).sorted().toList());
    }
  }

  @Test
  void testRunWithStepOneToMany() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1", "item2");
      List<StepBase> steps = List.of(new TestSteps.TestStepOneToMany());

      Multi<Object> result = runner.run(input, steps);

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(6));
      subscriber.awaitItems(6, Duration.ofSeconds(5));
      subscriber.assertItems("item1-1", "item1-2", "item1-3", "item2-1", "item2-2", "item2-3");
    }
  }

  @Test
  void testRunWithStepManyToMany() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<Object> input = Multi.createFrom().items("item1", "item2", "item3");
      List<StepBase> steps = List.of(new TestSteps.TestStepManyToMany());

      Multi<Object> result = runner.run(input, steps);

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(3));
      subscriber.awaitItems(3, Duration.ofSeconds(5));
      subscriber.assertItems("Streamed: item1", "Streamed: item2", "Streamed: item3");
    }
  }

  @Test
  void testRunWithStepOneToAsync() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1", "item2", "item3");
      List<StepBase> steps = List.of(new TestSteps.TestStepOneToAsync());

      Multi<Object> result = runner.run(input, steps);

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(3));
      subscriber.awaitItems(3, Duration.ofSeconds(5));
      subscriber.assertItems("Async: item1", "Async: item2", "Async: item3");
    }
  }

  @Test
  void testRunWithMultipleSteps() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1", "item2");
      List<StepBase> steps =
          List.of(new TestSteps.TestStepOneToOne(), new TestSteps.TestStepOneToMany());

      Multi<Object> result = runner.run(input, steps);

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(6));
      subscriber.awaitItems(6, Duration.ofSeconds(5)).assertCompleted();

      // Grab all items
      List<Object> actualItems = subscriber.getItems();

      // Expected items
      Set<String> expectedItems =
          Set.of(
              "Processed: item1-1",
              "Processed: item1-2",
              "Processed: item1-3",
              "Processed: item2-1",
              "Processed: item2-2",
              "Processed: item2-3");

      // Assert ignoring order
      assertEquals(expectedItems, new HashSet<>(actualItems));
    }
  }

  @Test
  void testRunWithFailingStepAndRecovery() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1", "item2");
      List<StepBase> steps = List.of(new TestSteps.FailingStep(true));

      Multi<Object> result = runner.run(input, steps);

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(2));
      subscriber.awaitItems(2, Duration.ofSeconds(5)).assertCompleted();

      // With recovery enabled, items should pass through unchanged
      // Order may vary due to asynchronous processing
      List<Object> actualItems = subscriber.getItems();

      // compare ignoring order
      assertEquals(
          Set.of("item1", "item2"), // expected as a set
          new HashSet<>(actualItems) // actual as a set
          );
    }
  }

  @Test
  void testRunWithFailingStepWithoutRecovery() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1", "item2");
      List<StepBase> steps = List.of(new TestSteps.FailingStep());

      Multi<Object> result = runner.run(input, steps);

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(1));
      subscriber.awaitFailure(Duration.ofSeconds(5));

      // Without recovery, should fail
      assertInstanceOf(RuntimeException.class, subscriber.getFailure());
      assertEquals("Intentional failure for testing", subscriber.getFailure().getMessage());
    }
  }

  @Test
  void testRetryMechanismWithSuccess() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1");
      RetryTestSteps.AsyncFailNTimesStep step = new RetryTestSteps.AsyncFailNTimesStep(2);

      // Configure retry settings
      step.liveConfig().overrides().retryLimit(3).retryWait(Duration.ofMillis(10));

      Multi<Object> result = runner.run(input, List.of((StepBase) step));

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(1));
      subscriber.awaitItems(1, Duration.ofSeconds(5));

      // Should succeed after 2 failures and 1 success
      subscriber.assertItems("Async Success: item1");
      assertEquals(3, step.getCallCount()); // 2 failures + 1 success
    }
  }

  @Test
  void testRetryMechanismWithFailure() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1");
      RetryTestSteps.AsyncFailNTimesStep step = new RetryTestSteps.AsyncFailNTimesStep(3);

      // Configure retry settings - only 2 retries, but need 3 failures to pass
      step.liveConfig().overrides().retryLimit(2).retryWait(Duration.ofMillis(10));

      Multi<Object> result = runner.run(input, List.of((StepBase) step));

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(1));
      subscriber.awaitFailure(Duration.ofSeconds(5));

      // Should fail after 2 retries (3 total attempts)
      assertInstanceOf(RuntimeException.class, subscriber.getFailure());
      assertEquals("Intentional async failure #3", subscriber.getFailure().getMessage());
      assertEquals(3, step.getCallCount()); // 2 retries + 1 initial = 3 total calls
    }
  }

  @Test
  void testRetryWithRecovery() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1");
      RetryTestSteps.AsyncFailNTimesStep step = new RetryTestSteps.AsyncFailNTimesStep(3);

      // Configure recovery and retry settings
      step.liveConfig()
          .overrides()
          .recoverOnFailure(true)
          .retryLimit(2)
          .retryWait(Duration.ofMillis(10));

      Multi<Object> result = runner.run(input, List.of((StepBase) step));

      AssertSubscriber<Object> subscriber =
          result.subscribe().withSubscriber(AssertSubscriber.create(1));
      subscriber.awaitItems(1, Duration.ofSeconds(5));

      // Should recover after 2 retries (3 total attempts)
      subscriber.assertItems("item1"); // Original item passed through on recovery
      assertEquals(3, step.getCallCount()); // 2 retries + 1 initial = 3 total calls
    }
  }

  @Test
  void testRunWithUnknownStepType() {
    try (PipelineRunner runner = new PipelineRunner()) {
      Multi<String> input = Multi.createFrom().items("item1", "item2");

      // Create a mock step that doesn't implement any known step interface
      // We'll create a StepBase that doesn't match any of the expected types
      StepBase unknownStep = StepConfig::new;

      assertThrows(IllegalArgumentException.class, () -> runner.run(input, List.of(unknownStep)));
    }
  }

  @Test
  void testConfigurationIntegration() {
    try (PipelineRunner _ = new PipelineRunner()) {
      Multi.createFrom().items("item1");

      // Create a step with specific configuration
      TestSteps.TestStepOneToOne step = new TestSteps.TestStepOneToOne();
      step.liveConfig().overrides().retryLimit(5).retryWait(Duration.ofMillis(100)).debug(true);

      assertEquals(5, step.retryLimit());
      assertEquals(Duration.ofMillis(100), step.retryWait());
      assertTrue(step.debug());
    }
  }
}
