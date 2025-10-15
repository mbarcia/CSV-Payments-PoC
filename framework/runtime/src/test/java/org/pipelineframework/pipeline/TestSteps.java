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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.pipelineframework.step.ConfigurableStep;
import org.pipelineframework.step.StepManyToMany;
import org.pipelineframework.step.StepOneToMany;
import org.pipelineframework.step.StepOneToOne;
import org.pipelineframework.step.blocking.StepOneToOneBlocking;

@ApplicationScoped
public class TestSteps {

    private static final Logger LOG = Logger.getLogger(TestSteps.class);

    public static class TestStepOneToOneBlocking extends ConfigurableStep
            implements StepOneToOneBlocking<String, String> {
        private boolean hasManualConfig = false;
        private int manualRetryLimit = -1;
        private java.time.Duration manualRetryWait = null;
        private boolean manualDebug = false;

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
        public void initialiseWithConfig(org.pipelineframework.config.LiveStepConfig config) {
            // Check if this is the first time being configured with non-default values
            // If so, preserve these as manual configuration
            if (!hasManualConfig && config != null) {
                // Check if the incoming config has custom values
                if (config.retryLimit()
                                != new org.pipelineframework.config.StepConfig().retryLimit()
                        || config.retryWait()
                                != new org.pipelineframework.config.StepConfig().retryWait()
                        || config.debug()
                                != new org.pipelineframework.config.StepConfig().debug()) {
                    // This looks like manual configuration - save the values
                    setManualConfig(config.retryLimit(), config.retryWait(), config.debug());
                }
            }

            if (hasManualConfig) {
                // If we have manual config, apply it on top of the new config
                super.initialiseWithConfig(config);
                // Apply the manual overrides
                if (config != null) {
                    config.overrides()
                            .retryLimit(manualRetryLimit)
                            .retryWait(manualRetryWait)
                            .debug(manualDebug);
                }
            } else {
                super.initialiseWithConfig(config);
            }
        }

        // Method to mark that manual config has been set
        public void setManualConfig(int retryLimit, java.time.Duration retryWait, boolean debug) {
            this.hasManualConfig = true;
            this.manualRetryLimit = retryLimit;
            this.manualRetryWait = retryWait;
            this.manualDebug = debug;
        }

        public int retryLimit() {
            return effectiveConfig().retryLimit();
        }

        public java.time.Duration retryWait() {
            return effectiveConfig().retryWait();
        }

        public boolean debug() {
            return effectiveConfig().debug();
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
        }

        @Override
        public Uni<String> apply(String input) {
            throw new RuntimeException("Intentional failure for testing");
        }

        /**
         * Handle a failed item by logging the dead-letter event and returning the original item unchanged.
         *
         * @param failedItem a Uni that produces the item that failed processing
         * @param cause the throwable that caused the failure
         * @return a Uni that emits the original input value
         */
        @Override
        public Uni<String> deadLetter(Uni<String> failedItem, Throwable cause) {
            LOG.infof("Dead letter handled for: %s", failedItem.toString());
            // Return the original input value when recovery is enabled
            return failedItem.onItem().transform(item -> item);
        }

        @Override
        public void initialiseWithConfig(org.pipelineframework.config.LiveStepConfig config) {
            super.initialiseWithConfig(config);
            // Apply the recovery setting after the config is properly set up
            if (shouldRecover && config != null) {
                config.overrides().recoverOnFailure(true);
            }
        }

        public Uni<String> applyOneToOne(String input) {
            return apply(input);
        }
    }
}