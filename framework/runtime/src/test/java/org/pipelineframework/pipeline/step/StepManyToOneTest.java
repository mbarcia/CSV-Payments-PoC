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

package org.pipelineframework.pipeline.step;

import static org.junit.jupiter.api.Assertions.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.step.StepManyToOne;

class StepManyToOneTest {

    static class TestStep implements StepManyToOne<String, String> {
        @Override
        public Uni<String> applyBatchMulti(Multi<String> inputs) {
            return inputs.collect()
                    .asList()
                    .onItem()
                    .transform(list -> "Batch processed: " + String.join(", ", list));
        }

        @Override
        public org.pipelineframework.config.StepConfig effectiveConfig() {
            // Return an empty StepConfig so the method defaults are not overridden in the
            // applyReduce method
            return new org.pipelineframework.config.StepConfig();
        }

        @Override
        public void initialiseWithConfig(org.pipelineframework.config.LiveStepConfig config) {
            // Use the config provided
        }

        @Override
        public int batchSize() {
            return 2; // Small batch size for testing
        }

        @Override
        public Duration batchTimeout() {
            return Duration.ofMillis(1000); // Default timeout
        }
    }

    static class ConfiguredTestStep implements StepManyToOne<String, String> {
        private StepConfig config = new StepConfig();

        public ConfiguredTestStep withBatchSize(int batchSize) {
            this.config.batchSize(batchSize);
            return this;
        }

        public ConfiguredTestStep withBatchTimeout(Duration timeout) {
            this.config.batchTimeout(timeout);
            return this;
        }

        @Override
        public Uni<String> applyBatchMulti(Multi<String> inputs) {
            return inputs.collect()
                    .asList()
                    .onItem()
                    .transform(list -> "Batch processed: " + String.join(", ", list));
        }

        @Override
        public StepConfig effectiveConfig() {
            return config;
        }

        @Override
        public void initialiseWithConfig(org.pipelineframework.config.LiveStepConfig config) {
            // Use the config provided
        }

        @Override
        public int batchSize() {
            return 10; // Default batch size that should be overridden by config
        }
    }

    @Test
    void testApplyBatchMethod() {
        // Given
        TestStep step = new TestStep();
        Multi<String> inputs = Multi.createFrom().items("item1", "item2", "item3");

        // When
        Uni<String> result = step.applyBatchMulti(inputs);

        // Then
        String value = result.await().indefinitely();
        assertEquals("Batch processed: item1, item2, item3", value);
    }

    @Test
    void testApplyMethod() {
        // Given - TestStep.effectiveConfig() returns new StepConfig() which has default batch size
        // of 10
        TestStep step = new TestStep();
        Multi<String> input = Multi.createFrom().items("item1", "item2", "item3", "item4");

        // When
        Uni<String> result = step.applyReduce(input);

        // Then - with default StepConfig batch size (10), all 4 items go in one batch
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitItem(Duration.ofSeconds(5));
        // All items processed in one batch due to StepConfig's default batch size
        subscriber.assertItem("Batch processed: item1, item2, item3, item4");
    }

    @Test
    void testApplyMethodWithConfiguredBatchSize() {
        // Given
        ConfiguredTestStep step = new ConfiguredTestStep().withBatchSize(3);
        Multi<String> input =
                Multi.createFrom().items("item1", "item2", "item3", "item4", "item5", "item6");

        // When
        Uni<String> result = step.applyReduce(input);

        // Then
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitItem(Duration.ofSeconds(5));
        // With configured batch size of 3, we expect 2 batches: ["item1", "item2", "item3"] and
        // ["item4", "item5", "item6"]
        // The applyReduce method uses .collect().last(), so only the last batch result is returned
        subscriber.assertItem("Batch processed: item4, item5, item6");
    }

    @Test
    void testApplyMethodWithLargeConfiguredBatchSize() {
        // Given
        ConfiguredTestStep step = new ConfiguredTestStep().withBatchSize(10); // Larger than input
        Multi<String> input = Multi.createFrom().items("item1", "item2", "item3");

        // When
        Uni<String> result = step.applyReduce(input);

        // Then
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitItem(Duration.ofSeconds(5));
        // With configured batch size of 10, all 3 items should be in one batch
        subscriber.assertItem("Batch processed: item1, item2, item3");
    }

    @Test
    void testApplyMethodWithConfiguredBatchTimeout() {
        // Given
        ConfiguredTestStep step =
                new ConfiguredTestStep()
                        .withBatchSize(100) // Very large batch size
                        .withBatchTimeout(Duration.ofMillis(250)); // Longer timeout
        // Create a stream that emits items slowly to test timeout
        Multi<String> input =
                Multi.createFrom()
                        .items("item1", "item2", "item3")
                        .onItem()
                        .call(
                                item ->
                                        Uni.createFrom()
                                                .item(item)
                                                .onItem()
                                                .delayIt()
                                                .by(Duration.ofMillis(50))); // Delay between items

        // When
        Uni<String> result = step.applyReduce(input);

        // Then - With longer timeout, all items should be in one batch
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitItem(Duration.ofSeconds(5));
        // With batch size of 100 and timeout of 250ms, all 3 items should be in one batch
        subscriber.assertItem("Batch processed: item1, item2, item3");
    }

    @Test
    void testApplyMethodUsesConfiguredValuesOverDefaults() {
        // Given - Test that configured values take precedence over method defaults
        ConfiguredTestStep step = new ConfiguredTestStep().withBatchSize(5); // Configured value
        Multi<String> input =
                Multi.createFrom().items("item1", "item2", "item3", "item4", "item5", "item6");

        // When
        Uni<String> result = step.applyReduce(input);

        // Then - Should use configured batch size (5) not the default from the method (10)
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitItem(Duration.ofSeconds(5));
        // With batch size of 5, first batch is ["item1", "item2", "item3", "item4", "item5"],
        // second batch is ["item6"]
        // The applyReduce method uses .collect().last(), so only the last batch result is returned
        subscriber.assertItem(
                "Batch processed: item6"); // Only the last batch (with just item6) is returned
    }

    @Test
    void testConfiguredBatchSizeOverrideWithSmallerSize() {
        // Given - Test with a smaller batch size to ensure proper batching
        ConfiguredTestStep step = new ConfiguredTestStep().withBatchSize(2); // Small batch size
        Multi<String> input =
                Multi.createFrom().items("item1", "item2", "item3", "item4", "item5", "item6");

        // When
        Uni<String> result = step.applyReduce(input);

        // Then - Batches of size 2: ["item1", "item2"], ["item3", "item4"], ["item5", "item6"]
        // Last batch is ["item5", "item6"]
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitItem(Duration.ofSeconds(5));
        subscriber.assertItem("Batch processed: item5, item6"); // Last batch result
    }

    @Test
    void testDefaultStepConfigValues() {
        // Given - TestStep.effectiveConfig() returns StepConfig (with defaults), not "no config"
        // The StepConfig has default batch size of 10, so all items go in one batch
        TestStep step = new TestStep();
        Multi<String> input = Multi.createFrom().items("item1", "item2", "item3", "item4");

        // When
        Uni<String> result = step.applyReduce(input);

        // Then - Uses StepConfig's default batch size (10), so all items in one batch
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitItem(Duration.ofSeconds(5));
        // With batch size of 10, all 4 items are processed in one batch
        subscriber.assertItem("Batch processed: item1, item2, item3, item4");
    }

    @Test
    void testCsvProcessingUseCase_BatchSizeConfiguration() {
        // Given - Simulate the CSV processing scenario where we want to process all related records
        // together
        ConfiguredTestStep step =
                new ConfiguredTestStep()
                        .withBatchSize(
                                50) // Large enough to handle all records for a single CSV file
                        .withBatchTimeout(
                                Duration.ofSeconds(5)); // Generous timeout to allow accumulation
        Multi<String> input =
                Multi.createFrom()
                        .range(1, 13) // 12 items simulating 12 PaymentOutput records
                        .map(i -> "payment_" + i + "_for_csv_file_X");

        // When
        Uni<String> result = step.applyReduce(input);

        // Then - All 12 items should be processed together in one batch
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitItem(Duration.ofSeconds(5));
        // All items should be in one batch result
        String resultString = subscriber.getItem();
        assertTrue(resultString.contains("payment_1_for_csv_file_X"));
        assertTrue(resultString.contains("payment_12_for_csv_file_X"));
        // The result should contain all 12 items
        assertTrue(
                resultString.contains(
                        "payment_1_for_csv_file_X, payment_2_for_csv_file_X, payment_3_for_csv_file_X"));
    }

    @Test
    void testApplyMethodWithEmptyStream() {
        // Given - Empty input stream
        TestStep step = new TestStep();
        Multi<String> input = Multi.createFrom().empty();

        // When
        Uni<String> result = step.applyReduce(input);

        // Then - Should fail with IllegalArgumentException
        io.smallrye.mutiny.helpers.test.UniAssertSubscriber<String> subscriber =
                result.subscribe()
                        .withSubscriber(
                                io.smallrye.mutiny.helpers.test.UniAssertSubscriber.create());
        subscriber.awaitFailure(Duration.ofSeconds(5));
        // Verify that the failure is an IllegalArgumentException with the expected message
        Throwable failure = subscriber.getFailure();
        assertNotNull(failure);
        assertTrue(failure instanceof IllegalArgumentException);
        assertEquals("Empty input not allowed for StepManyToOne", failure.getMessage());
    }
}
