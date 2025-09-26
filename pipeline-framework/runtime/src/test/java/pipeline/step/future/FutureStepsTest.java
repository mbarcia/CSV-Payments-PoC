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

package pipeline.step.future;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.PipelineRunner;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FutureStepsTest {

    PipelineRunner pipelineRunner = new PipelineRunner();

    @Test
    void testCompletableFutureStep() {
        // Given: Create test data
        Multi<String> input = Multi.createFrom().items("Payment1", "Payment2", "Payment3");

        // Create steps
        ValidatePaymentStepBlocking validateStep = new ValidatePaymentStepBlocking();
        validateStep.liveConfig().overrides().autoPersist(false);

        ProcessPaymentFutureStep processStep = new ProcessPaymentFutureStep();
        processStep.liveConfig().overrides().autoPersist(false);

        // When: Run pipeline
        Multi<String> result =
                (Multi<String>) pipelineRunner.run(input, List.of(validateStep, processStep));
        AssertSubscriber<String> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(3));

        // Then: Verify results
        subscriber.awaitItems(3).awaitCompletion(Duration.ofSeconds(10));

        List<String> results = subscriber.getItems();
        assertEquals(3, results.size());

        // Verify all items were processed
        assertTrue(results.contains("Processed: Validated: Payment1"));
        assertTrue(results.contains("Processed: Validated: Payment2"));
        assertTrue(results.contains("Processed: Validated: Payment3"));
    }

    // Helper step for validating payments
    public static class ValidatePaymentStepBlocking extends ConfigurableStep
            implements StepOneToOneBlocking<String, String> {

        @Override
        public io.smallrye.mutiny.Uni<String> apply(String payment) {
            // Simulate some processing time
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return io.smallrye.mutiny.Uni.createFrom().item("Validated: " + payment);
        }

        @Override
        public io.smallrye.mutiny.Uni<String> applyOneToOne(String input) {
            return apply(input);
        }

        @Override
        public io.github.mbarcia.pipeline.config.StepConfig effectiveConfig() {
            return new io.github.mbarcia.pipeline.config.StepConfig();
        }

        @Override
        public void initialiseWithConfig(io.github.mbarcia.pipeline.config.LiveStepConfig config) {
            // Use the config provided
        }
    }
}
