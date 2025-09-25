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

package pipeline.step.future;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.step.future.StepOneToOneCompletableFuture;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class StepOneToOneCompletableFutureTest {

    static class TestStepFuture implements StepOneToOneCompletableFuture<String, String> {
        @Override
        public CompletableFuture<String> applyAsync(String input) {
            return CompletableFuture.completedFuture("Future processed: " + input);
        }

        @Override
        public StepConfig effectiveConfig() {
            return new StepConfig();
        }
    }

    @Test
    void testApplyAsyncMethod() {
        // Given
        TestStepFuture step = new TestStepFuture();

        // When
        CompletableFuture<String> result = step.applyAsync("test");

        // Then
        String value = result.join();
        assertEquals("Future processed: test", value);
    }

    @Test
    void testDefaultExecutor() {
        // Given
        TestStepFuture step = new TestStepFuture();

        // When
        var executor = step.getExecutor();

        // Then
        assertNotNull(executor);
    }

    @Test
    void testDefaultConcurrency() {
        // Given
        TestStepFuture step = new TestStepFuture();

        // When
        int concurrency = step.concurrency();

        // Then
        assertEquals(1, concurrency);
    }

    @Test
    void testDefaultRunWithVirtualThreads() {
        // Given
        TestStepFuture step = new TestStepFuture();

        // When
        boolean runWithVirtualThreads = step.runWithVirtualThreads();

        // Then
        assertFalse(runWithVirtualThreads);
    }

    @Test
    void testApplyMethodInStep() {
        // Given
        TestStepFuture step = new TestStepFuture();
        Multi<Object> input = Multi.createFrom().items("item1", "item2");

        // When
        Multi<Object> result = step.apply(input);

        // Then
        AssertSubscriber<Object> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(2));
        subscriber.awaitItems(2, Duration.ofSeconds(5));
        subscriber.assertItems("Future processed: item1", "Future processed: item2");
    }
}
