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

package pipeline.step.blocking;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.step.blocking.StepOneToManyBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class StepOneToManyBlockingTest {

    static class TestStepBlocking implements StepOneToManyBlocking<String, String> {
        @Override
        public List<String> applyList(String input) {
            return List.of(input + "-1", input + "-2", input + "-3");
        }

        @Override
        public StepConfig effectiveConfig() {
            return new StepConfig();
        }
    }

    @Test
    void testApplyListMethod() {
        // Given
        TestStepBlocking step = new TestStepBlocking();

        // When
        List<String> result = step.applyList("test");

        // Then
        assertEquals(List.of("test-1", "test-2", "test-3"), result);
    }

    @Test
    void testDefaultConcurrency() {
        // Given
        TestStepBlocking step = new TestStepBlocking();

        // When
        int concurrency = step.concurrency();

        // Then
        assertEquals(1, concurrency);
    }

    @Test
    void testDefaultRunWithVirtualThreads() {
        // Given
        TestStepBlocking step = new TestStepBlocking();

        // When
        boolean runWithVirtualThreads = step.runWithVirtualThreads();

        // Then
        assertFalse(runWithVirtualThreads);
    }

    @Test
    void testApplyMethodInStep() {
        // Given
        TestStepBlocking step = new TestStepBlocking();
        Multi<Object> input = Multi.createFrom().items("item1", "item2");

        // When
        Multi<Object> result = step.apply(input);

        // Then
        AssertSubscriber<Object> subscriber =
                result.subscribe().withSubscriber(AssertSubscriber.create(6));
        subscriber.awaitItems(6, Duration.ofSeconds(5));
        subscriber.assertItems("item1-1", "item1-2", "item1-3", "item2-1", "item2-2", "item2-3");
    }
}
