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

import io.smallrye.mutiny.Uni;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.logging.Logger;
import org.pipelineframework.step.ConfigurableStep;
import org.pipelineframework.step.StepOneToOne;

public class RetryTestSteps {

    private static final Logger LOG = Logger.getLogger(RetryTestSteps.class);

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
                throw new RuntimeException("Intentional async failure #" + callCount.get());
            }
            return Uni.createFrom().item("Async Success: " + input);
        }

        private boolean hasManualConfig = false;
        private int manualRetryLimit = -1;
        private java.time.Duration manualRetryWait = null;
        private boolean manualDebug = false;
        private boolean manualRecoverOnFailure = false;

        /**
         * Handles a failed asynchronous item by logging the dead-letter event and returning the original item.
         *
         * @param failedItem a Uni that contains the item which failed processing
         * @param cause the exception that caused the failure
         * @return the original item value extracted from {@code failedItem}
         */
        @Override
        public Uni<String> deadLetter(Uni<String> failedItem, Throwable cause) {
            LOG.infof(
                    "AsyncFailedNTimesStep dead letter: %s due to %s",
                    failedItem, cause.getMessage());
            // For recovery, return the original input value from the Uni
            return failedItem.onItem().transform(item -> item);
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
                        || config.debug() != new org.pipelineframework.config.StepConfig().debug()
                        || config.recoverOnFailure()
                                != new org.pipelineframework.config.StepConfig()
                                        .recoverOnFailure()) {
                    // This looks like manual configuration - save the values
                    setManualConfig(
                            config.retryLimit(),
                            config.retryWait(),
                            config.debug(),
                            config.recoverOnFailure());
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
                            .debug(manualDebug)
                            .recoverOnFailure(manualRecoverOnFailure);
                }
            } else {
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
            this.manualRecoverOnFailure = recoverOnFailure;
        }

        public int getCallCount() {
            return callCount.get();
        }
    }
}