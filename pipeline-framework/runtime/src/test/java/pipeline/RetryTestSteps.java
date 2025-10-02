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
import io.github.mbarcia.pipeline.step.StepOneToOne;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryTestSteps {

    public static class FailBlockingNTimesStep extends ConfigurableStep
            implements StepOneToOneBlocking<String, String> {
        private final int failCount;
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final boolean shouldRecover;

        public FailBlockingNTimesStep(int failCount) {
            this(failCount, false);
        }

        public FailBlockingNTimesStep(int failCount, boolean shouldRecover) {
            this.failCount = failCount;
            this.shouldRecover = shouldRecover;
            // Don't call liveConfig() here as the step isn't fully initialized yet
            // The configuration will be applied externally when the step is used in a pipeline
        }

        @Override
        public Uni<String> apply(String input) {
            if (callCount.incrementAndGet() <= failCount) {
                throw new RuntimeException(
                        "Intentional failure for testing - attempt " + callCount.get());
            }
            return Uni.createFrom().item("Success: " + input);
        }

        @Override
        public void initialiseWithConfig(io.github.mbarcia.pipeline.config.LiveStepConfig config) {
            super.initialiseWithConfig(config);
            // Apply the recovery setting after the config is properly set up
            if (shouldRecover) {
                if (config != null) {
                    config.overrides().recoverOnFailure(true);
                }
            }
        }

        public Uni<String> applyOneToOne(String input) {
            return apply(input);
        }

        @Override
        public Uni<String> deadLetter(Uni<String> failedItem, Throwable cause) {
            System.out.println("DLQ: " + failedItem.toString() + " due to " + cause.getMessage());
            return Uni.createFrom().nullItem();
        }
    }

    public static class AsyncFailNTimesStep extends ConfigurableStep
            implements StepOneToOne<String, String> {
        private final int failCount;
        private final AtomicInteger callCount = new AtomicInteger(0);

        public AsyncFailNTimesStep(int failCount) {
            this.failCount = failCount;
        }

        @Override
        public Uni<String> applyOneToOne(String input) {
            if (callCount.incrementAndGet() <= failCount) {
                throw new RuntimeException("Intentional async failure # " + callCount.get());
            }
            return Uni.createFrom().item("Async Success: " + input);
        }

        public int getCallCount() {
            return callCount.get();
        }
    }
}
