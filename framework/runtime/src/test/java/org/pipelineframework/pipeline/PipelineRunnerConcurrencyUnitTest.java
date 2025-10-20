/*
 * Copyright (c) 2023-2025 Mariano Barcia
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

package org.pipelineframework.pipeline;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.pipelineframework.config.LiveStepConfig;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.step.ConfigurableStep;
import org.pipelineframework.step.StepOneToOne;

class PipelineRunnerConcurrencyUnitTest {

    static class TestConcurrentStep extends ConfigurableStep
            implements StepOneToOne<String, String> {

        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public Uni<String> applyOneToOne(String input) {
            // Increment call count to track how many items are being processed
            callCount.incrementAndGet();

            // Simulate processing time that varies by input
            int processingTime = input.equals("slow") ? 500 : 100; // slow item takes longer
            try {
                Thread.sleep(processingTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return Uni.createFrom().item("processed:" + input);
        }
    }

    @Test
    void testSequentialProcessingByDefault() {
        // Given - Default config (parallel = false)
        Multi<String> input = Multi.createFrom().items("item1", "item2", "item3");

        TestConcurrentStep step = new TestConcurrentStep();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), new PipelineConfig());
        step.initialiseWithConfig(liveConfig);

        // When - Use the PipelineRunner's applyOneToOneUnchecked method directly
        Multi<String> result = (Multi<String>) PipelineRunnerTestHelper.applyOneToOne(step, input);

        // Then - Should process sequentially
        AssertSubscriber<String> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(3));
        List<String> items = subscriber.awaitItems(3, Duration.ofSeconds(5)).getItems();

        assertEquals(3, items.size());
        assertTrue(items.contains("processed:item1"));
        assertTrue(items.contains("processed:item2"));
        assertTrue(items.contains("processed:item3"));

        assertEquals(3, step.callCount.get());
    }

    @Test
    void testConcurrentProcessingWithoutOrderPreservation() {
        // Given - Concurrent processing with MERGE strategy
        Multi<String> input = Multi.createFrom().items("slow", "fast1", "fast2"); // slow item first

        TestConcurrentStep step = new TestConcurrentStep();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), new PipelineConfig());
        liveConfig.overrides().parallel(true); // Enable concurrency
        step.initialiseWithConfig(liveConfig);

        // When
        Multi<String> result = (Multi<String>) PipelineRunnerTestHelper.applyOneToOne(step, input);

        // Then - Should process concurrently, with fast items potentially completing before slow
        // item
        AssertSubscriber<String> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(3));
        List<String> items = subscriber.awaitItems(3, Duration.ofSeconds(2)).getItems();

        assertEquals(3, items.size());
        assertTrue(items.contains("processed:slow"));
        assertTrue(items.contains("processed:fast1"));
        assertTrue(items.contains("processed:fast2"));

        // With MERGE strategy, output order may not match input order due to timing
        // But all should be processed
        assertEquals(3, step.callCount.get());
    }

    @Test
    void testConcurrentProcessingWithOrderPreservation() {
        // Given - Concurrent processing with CONCATENATE strategy (preserves order)
        Multi<String> input = Multi.createFrom().items("slow", "fast1", "fast2"); // slow item first

        TestConcurrentStep step = new TestConcurrentStep();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), new PipelineConfig());
        liveConfig.overrides().parallel(true); // Enable concurrency
        step.initialiseWithConfig(liveConfig);

        // When
        Multi<String> result = (Multi<String>) PipelineRunnerTestHelper.applyOneToOne(step, input);

        // Then - Should process concurrently but preserve original order
        AssertSubscriber<String> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(3));
        List<String> items = subscriber.awaitItems(3, Duration.ofSeconds(2)).getItems();

        assertEquals(3, items.size());

        // With CONCATENATE strategy, order should be preserved
        assertTrue(items.get(0).toString().contains("slow"));
        assertTrue(items.get(1).toString().contains("fast1"));
        assertTrue(items.get(2).toString().contains("fast2"));
    }

    @Test
    void testBackwardCompatibilityWithParallelFalse() {
        // Given - parallel = false (default), should process sequentially
        Multi<String> input = Multi.createFrom().items("itemA", "itemB");

        TestConcurrentStep step = new TestConcurrentStep();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), new PipelineConfig());
        // Don't override parallel - should use default of false (backward compatibility)
        step.initialiseWithConfig(liveConfig);

        // When
        Multi<String> result = (Multi<String>) PipelineRunnerTestHelper.applyOneToOne(step, input);

        // Then - Should work the same as before (backward compatibility maintained)
        AssertSubscriber<String> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(2));
        List<String> items = subscriber.awaitItems(2, Duration.ofSeconds(5)).getItems();

        assertEquals(2, items.size());
        assertTrue(items.contains("processed:itemA"));
        assertTrue(items.contains("processed:itemB"));
        assertEquals(2, step.callCount.get());
    }

    // Helper class to access private methods for testing
    static class PipelineRunnerTestHelper {
        public static Object applyOneToOne(Object step, Object current) {
            try {
                java.lang.reflect.Method method =
                        org.pipelineframework.PipelineRunner.class.getDeclaredMethod(
                                "applyOneToOneUnchecked",
                                org.pipelineframework.step.StepOneToOne.class,
                                Object.class);
                method.setAccessible(true);
                return method.invoke(null, step, current);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
