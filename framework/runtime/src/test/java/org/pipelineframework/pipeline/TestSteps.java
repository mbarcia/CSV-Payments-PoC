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
                final org.pipelineframework.config.StepConfig defaultCfg =
                        new org.pipelineframework.config.StepConfig();
                if (config.retryLimit() != defaultCfg.retryLimit()
                        || !java.util.Objects.equals(config.retryWait(), defaultCfg.retryWait())
                        || config.debug() != defaultCfg.debug()) {
                    // This looks like manual configuration - save the values
                    setManualConfig(config.retryLimit(), config.retryWait(), config.debug());
                }
            }

            if (hasManualConfig) {
                if (config != null) {
                    config.overrides()
                            .retryLimit(manualRetryLimit)
                            .retryWait(manualRetryWait)
                            .debug(manualDebug);
                }
                super.initialiseWithConfig(config);
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
        // Configuration preservation fields like in AsyncFailNTimesStep
        private boolean hasManualConfig = false;
        private int manualRetryLimit = -1;
        private java.time.Duration manualRetryWait = null;
        private boolean manualDebug = false;
        private boolean manualRecoverOnFailure = false;
        private boolean manualRecoverOnFailureSet = false; // Sentinel to track if constructor set the value

        public FailingStepBlocking() {
            this(false);
        }

        public FailingStepBlocking(boolean shouldRecover) {
            this.manualRecoverOnFailure = shouldRecover;
            this.manualRecoverOnFailureSet = true; // Mark that constructor explicitly set this value
        }
        @Override
        public Uni<String> apply(String input) {
            // Return the input wrapped in a Uni that fails - this way the input is preserved
            // for potential recovery by the deadLetter method
            return Uni.createFrom()
                    .failure(new RuntimeException("Intentional failure for testing"));
        }

        @Override
        public Uni<String> applyOneToOne(String input) {
            // For the reactive interface, call the blocking interface method
            // This ensures both interfaces are properly handled
            return apply(input);
        }

        /**
         * Handle a failed item by logging the dead-letter event with error context and returning
         * the original item unchanged. This method is invoked when a step fails and recovery is
         * enabled.
         *
         * @param failedItem a Uni that produces the item that failed processing
         * @param cause the throwable that caused the failure
         * @return a Uni that emits the original input value
         */
        @Override
        public Uni<String> deadLetter(Uni<String> failedItem, Throwable cause) {
            return failedItem
                    .onItem()
                    .invoke(
                            item ->
                                    LOG.infof(
                                            "Dead letter handled for item: %s, cause: %s",
                                            item, cause.getMessage()));
        }

        @Override
        public void initialiseWithConfig(org.pipelineframework.config.LiveStepConfig config) {
            // Check if this is the first time being configured with non-default values
            // If so, preserve these as manual configuration (like AsyncFailNTimesStep)
            if (!hasManualConfig && config != null) {
                // Check if the incoming config has custom values
                final org.pipelineframework.config.StepConfig defaultCfg =
                        new org.pipelineframework.config.StepConfig();
                boolean hasConfigRecoverOnFailure = config.recoverOnFailure() != defaultCfg.recoverOnFailure();
                if (config.retryLimit() != defaultCfg.retryLimit()
                        || !java.util.Objects.equals(config.retryWait(), defaultCfg.retryWait())
                        || config.debug() != defaultCfg.debug()
                        || hasConfigRecoverOnFailure) {
                    // This looks like manual configuration - save the values
                    // Only set recoverOnFailure from config if constructor didn't set it
                    boolean recoverOnFailureToUse = manualRecoverOnFailureSet ? manualRecoverOnFailure : config.recoverOnFailure();
                    setManualConfig(
                            config.retryLimit(),
                            config.retryWait(),
                            config.debug(),
                            recoverOnFailureToUse);
                }
            }

            if (hasManualConfig) {
                if (config != null) {
                    config.overrides()
                            .retryLimit(manualRetryLimit)
                            .retryWait(manualRetryWait)
                            .debug(manualDebug)
                            .recoverOnFailure(manualRecoverOnFailure);
                }
                super.initialiseWithConfig(config);
            } else {
                if (config != null) {
                    // Only apply config's recoverOnFailure if constructor didn't set it
                    if (!manualRecoverOnFailureSet) {
                        config.overrides().recoverOnFailure(config.recoverOnFailure());
                    } else {
                        config.overrides().recoverOnFailure(manualRecoverOnFailure);
                    }
                }
                super.initialiseWithConfig(config);
            }
        }

        // Method to mark that manual config has been set
        private void setManualConfig(
                int retryLimit,
                java.time.Duration retryWait,
                boolean debug,
                boolean recoverOnFailure) {
            this.hasManualConfig = true;
            this.manualRetryLimit = retryLimit;
            this.manualRetryWait = retryWait;
            this.manualDebug = debug;
            // Only update manualRecoverOnFailure if it wasn't set by constructor
            if (!manualRecoverOnFailureSet) {
                this.manualRecoverOnFailure = recoverOnFailure;
            }
        }
    }
}
