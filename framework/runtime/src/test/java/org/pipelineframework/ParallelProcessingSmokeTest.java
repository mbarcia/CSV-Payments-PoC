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

package org.pipelineframework;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.pipelineframework.config.LiveStepConfig;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.step.ConfigurableStep;
import org.pipelineframework.step.StepOneToOne;
import org.pipelineframework.step.future.StepOneToOneCompletableFuture;

@QuarkusTest
class ParallelProcessingSmokeTest {

    @Inject private PipelineRunner pipelineRunner;

    @Inject private PipelineConfig pipelineConfig;

    private static class TestStepOneToOne extends ConfigurableStep
            implements StepOneToOne<String, String> {

        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public Uni<String> applyOneToOne(String input) {
            // Simulate processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int count = callCount.incrementAndGet();
            return Uni.createFrom().item("processed:" + input + "_count" + count);
        }
    }

    private static class TestStepOneToOneCompletableFuture extends ConfigurableStep
            implements StepOneToOneCompletableFuture<String, String> {

        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public CompletableFuture<String> applyAsync(String input) {
            // Simulate processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int count = callCount.incrementAndGet();
            return CompletableFuture.completedFuture("processed:" + input + "_count" + count);
        }
    }

    @Test
    void testSequentialProcessingWorks() {
        // Given
        TestStepOneToOne step = new TestStepOneToOne();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), pipelineConfig);
        // Keep default parallel=false
        step.initialiseWithConfig(liveConfig);

        // When
        Multi<String> input = Multi.createFrom().items("item1", "item2", "item3");
        Multi<Object> result = (Multi<Object>) pipelineRunner.run(input, List.of(step));

        // Then
        AssertSubscriber<Object> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(3));
        subscriber.awaitItems(3, Duration.ofSeconds(5)).assertCompleted();

        List<Object> items = subscriber.getItems();
        assertEquals(3, items.size());
        assertTrue(items.contains("processed:item1_count1"));
        assertTrue(items.contains("processed:item2_count2"));
        assertTrue(items.contains("processed:item3_count3"));
    }

    @Test
    void testParallelProcessingWorks() {
        // Given
        TestStepOneToOne step = new TestStepOneToOne();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), pipelineConfig);
        liveConfig.overrides().parallel(true);
        step.initialiseWithConfig(liveConfig);

        // When
        Multi<String> input = Multi.createFrom().items("item1", "item2", "item3");
        Multi<Object> result = (Multi<Object>) pipelineRunner.run(input, List.of(step));

        // Then
        AssertSubscriber<Object> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(3));
        subscriber.awaitItems(3, Duration.ofSeconds(5)).assertCompleted();

        List<Object> items = subscriber.getItems();
        assertEquals(3, items.size());
        // With parallel processing, all items should be processed
        assertEquals(3, step.callCount.get());
    }

    @Test
    void testCompletableFutureParallelProcessingWorks() {
        // Given
        TestStepOneToOneCompletableFuture step = new TestStepOneToOneCompletableFuture();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), pipelineConfig);
        liveConfig.overrides().parallel(true);
        step.initialiseWithConfig(liveConfig);

        // When
        Multi<String> input = Multi.createFrom().items("item1", "item2", "item3");
        Multi<Object> result = (Multi<Object>) pipelineRunner.run(input, List.of(step));

        // Then
        AssertSubscriber<Object> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(3));
        subscriber.awaitItems(3, Duration.ofSeconds(5)).assertCompleted();

        List<Object> items = subscriber.getItems();
        assertEquals(3, items.size());
        // With parallel processing, all items should be processed
        assertEquals(3, step.callCount.get());
    }
}
