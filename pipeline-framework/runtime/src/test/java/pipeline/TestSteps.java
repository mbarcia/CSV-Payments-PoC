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

package pipeline;

import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.StepManyToMany;
import io.github.mbarcia.pipeline.step.StepOneToMany;
import io.github.mbarcia.pipeline.step.StepOneToOne;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestSteps {

    public static class TestStepOneToOneBlocking extends ConfigurableStep
            implements StepOneToOneBlocking<String, String> {
        @Override
        public Uni<String> apply(String input) {
            // This is a blocking operation that simulates processing
            try {
                Thread.sleep(10); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Uni.createFrom().item("Processed: " + input);
        }

        @Override
        public void initialiseWithConfig(io.github.mbarcia.pipeline.config.LiveStepConfig config) {
            super.initialiseWithConfig(config);
        }

        public Uni<String> applyOneToOne(String input) {
            return apply(input);
        }
    }

    public static class TestStepOneToMany extends ConfigurableStep
            implements StepOneToMany<String, String> {
        @Override
        public Multi<String> applyOneToMany(String input) {
            return Multi.createFrom().items(input + "-1", input + "-2", input + "-3");
        }
    }

    public static class TestStepManyToMany extends ConfigurableStep
            implements StepManyToMany<Object, Object> {
        @Override
        public Multi<Object> applyTransform(Multi<Object> input) {
            return input.onItem().transform(item -> "Streamed: " + item);
        }
    }

    public static class TestStepOneToOne extends ConfigurableStep
            implements StepOneToOne<String, String> {
        @Override
        public Uni<String> applyOneToOne(String input) {
            return Uni.createFrom().item("Async: " + input);
        }
    }

    public static class FailingStepBlocking extends ConfigurableStep
            implements StepOneToOneBlocking<String, String> {
        private final boolean shouldRecover;

        public FailingStepBlocking() {
            this(false);
        }

        public FailingStepBlocking(boolean shouldRecover) {
            this.shouldRecover = shouldRecover;
            if (shouldRecover) {
                liveConfig().overrides().recoverOnFailure(true);
            }
        }

        @Override
        public Uni<String> apply(String input) {
            throw new RuntimeException("Intentional failure for testing");
        }

        @Override
        public Uni<String> deadLetter(Uni<String> failedItem, Throwable cause) {
            System.out.println("Dead letter handled for: " + failedItem.toString());
            return Uni.createFrom().nullItem();
        }

        @Override
        public void initialiseWithConfig(io.github.mbarcia.pipeline.config.LiveStepConfig config) {
            super.initialiseWithConfig(config);
        }

        public Uni<String> applyOneToOne(String input) {
            return apply(input);
        }
    }
}
