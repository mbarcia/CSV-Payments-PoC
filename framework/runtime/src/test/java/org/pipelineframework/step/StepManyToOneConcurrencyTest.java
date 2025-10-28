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

package org.pipelineframework.step;

import static org.junit.jupiter.api.Assertions.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.pipelineframework.config.LiveStepConfig;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.step.blocking.StepManyToOneBlocking;

class StepManyToOneConcurrencyTest {

    static class TestReactiveManyToOneStep extends ConfigurableStep
            implements StepManyToOne<String, String> {

        private final AtomicInteger batchProcessingCount = new AtomicInteger(0);

        @Override
        public Uni<String> applyBatchMulti(Multi<String> inputs) {
            return inputs.collect()
                    .asList()
                    .onItem()
                    .transform(
                            list -> {
                                int batchNum = batchProcessingCount.incrementAndGet();
                                // Simulate some processing time
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                return "batch" + batchNum + ": " + String.join(",", list);
                            });
        }
    }

    static class TestBlockingManyToOneStep extends ConfigurableStep
            implements StepManyToOneBlocking<String, String> {

        private final AtomicInteger batchProcessingCount = new AtomicInteger(0);

        @Override
        public String applyBatchList(List<String> inputs) {
            int batchNum = batchProcessingCount.incrementAndGet();
            // Simulate some processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "batch" + batchNum + ": " + String.join(",", inputs);
        }
    }

    @Test
    void testReactiveStepManyToOneSequentialBatchesWithConcatenateStrategy() {
        // Given - CONCATENATE strategy processes batches sequentially
        Multi<String> input =
                Multi.createFrom().items("item1", "item2", "item3", "item4", "item5", "item6");

        TestReactiveManyToOneStep step = new TestReactiveManyToOneStep();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), new PipelineConfig());
        liveConfig
                .overrides()
                .batchSize(2) // Create batches of 2 items
                .parallel(false); // Sequential processing
        step.initialiseWithConfig(liveConfig);

        // When
        Uni<String> result = step.apply(input);

        // Then - Should complete successfully
        String output = result.await().atMost(Duration.ofSeconds(5));
        assertNotNull(output);
        // The final result should come from the last batch processed
        assertTrue(step.batchProcessingCount.get() >= 1); // At least one batch was processed
    }

    @Test
    void testReactiveStepManyToOneConcurrentBatchesWithMergeStrategy() {
        // Given - MERGE strategy processes batches concurrently
        Multi<String> input =
                Multi.createFrom().items("item1", "item2", "item3", "item4", "item5", "item6");

        TestReactiveManyToOneStep step = new TestReactiveManyToOneStep();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), new PipelineConfig());
        liveConfig
                .overrides()
                .batchSize(2) // Create batches of 2 items
                .parallel(true); // Concurrent processing
        step.initialiseWithConfig(liveConfig);

        // When
        Uni<String> result = step.apply(input);

        // Then - Should complete successfully, potentially faster due to concurrent processing
        String output = result.await().atMost(Duration.ofSeconds(5));
        assertNotNull(output);
        // The final result should come from the last batch processed
        assertTrue(step.batchProcessingCount.get() >= 1); // At least one batch was processed
    }

    @Test
    void testBlockingStepManyToOneSequentialBatchesWithConcatenateStrategy() {
        // Given - CONCATENATE strategy processes batches sequentially
        Multi<String> input =
                Multi.createFrom().items("item1", "item2", "item3", "item4", "item5", "item6");

        TestBlockingManyToOneStep step = new TestBlockingManyToOneStep();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), new PipelineConfig());
        liveConfig
                .overrides()
                .batchSize(2) // Create batches of 2 items
                .parallel(false); // Sequential processing
        step.initialiseWithConfig(liveConfig);

        // When
        Uni<String> result = step.apply(input);

        // Then - Should complete successfully
        String output = result.await().atMost(Duration.ofSeconds(5));
        assertNotNull(output);
        assertTrue(step.batchProcessingCount.get() >= 1); // At least one batch was processed
    }

    @Test
    void testBlockingStepManyToOneConcurrentBatchesWithMergeStrategy() {
        // Given - MERGE strategy processes batches concurrently
        Multi<String> input =
                Multi.createFrom().items("item1", "item2", "item3", "item4", "item5", "item6");

        TestBlockingManyToOneStep step = new TestBlockingManyToOneStep();
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), new PipelineConfig());
        liveConfig
                .overrides()
                .batchSize(2) // Create batches of 2 items
                .parallel(true); // Concurrent processing
        step.initialiseWithConfig(liveConfig);

        // When
        Uni<String> result = step.apply(input);

        // Then - Should complete successfully, potentially faster due to concurrent processing
        String output = result.await().atMost(Duration.ofSeconds(5));
        assertNotNull(output);
        assertTrue(step.batchProcessingCount.get() >= 1); // At least one batch was processed
    }
}
