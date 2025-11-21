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

package org.pipelineframework.config;

import io.quarkus.arc.Unremovable;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.Map;

/**
 * Configuration mapping for pipeline steps, supporting both global defaults
 * and per-step overrides using Quarkus configuration patterns.
 * <p>
 * To configure global defaults: pipeline.defaults.property=value
 * To configure specific steps: pipeline.step."fully.qualified.StepClass".property=value
 */
@ConfigMapping(prefix = "pipeline")
@Unremovable
public interface PipelineStepConfig {
    
    /**
     * Default configuration applied to all pipeline steps unless a step defines overrides.
     *
     * @return the global StepConfig used as defaults for steps
     */
    StepConfig defaults();
    
    /**
     * Per-step configuration overrides keyed by each step's fully qualified class name.
     *
     * <p>Configured under the prefix <code>pipeline.step."fully.qualified.class.name".property=value</code>.
     *
     * @return a map from fully qualified step class name to the corresponding {@link StepConfig} override
     */
    @WithName("step")
    Map<String, StepConfig> step();
    
    interface StepConfig {
        /**
         * Execution order for this step within the pipeline.
         *
         * @return the step order; 0 if not specified for a specific step.
         * Note: This value is not meaningful when used as global defaults (pipeline.defaults),
         * since global defaults should not define execution order.
         * When used in global defaults, this value is ignored since order is specific to each step.
         */
        @WithDefault("0")
        Integer order();

        /**
         * Maximum number of retry attempts for failed operations.
         * @return maximum retry attempts
         */
        @WithDefault("3")
        Integer retryLimit();

        /**
         * Base delay between retry attempts, in milliseconds.
         *
         * @return the base delay between retries in milliseconds
         */
        @WithDefault("2000")
        Long retryWaitMs();

        /**
         * Whether the step processes items in parallel.
         *
         * @return true if parallel processing is enabled, false otherwise.
         */
        @WithDefault("false")
        Boolean parallel();

        /**
         * Whether the step will attempt recovery after a failure.
         *
         * @return `true` if recovery is enabled, `false` otherwise
         */
        @WithDefault("false")
        Boolean recoverOnFailure();

        /**
         * Limit for backoff delays applied to retry attempts.
         *
         * @return the maximum backoff time in milliseconds
         */
        @WithDefault("30000")
        Long maxBackoff();

        /**
         * Whether jitter is added to retry delays.
         *
         * @return true to add jitter to retry delays, false otherwise.
         */
        @WithDefault("false")
        Boolean jitter();

        /**
         * Configures the capacity of the backpressure buffer for this pipeline step.
         *
         * @return the buffer capacity in number of items; default is 1024
         */
        @WithDefault("1024")
        Integer backpressureBufferCapacity();

        /**
         * Selects the backpressure strategy applied when buffering items.
         *
         * <p>Accepted values: "BUFFER" to buffer incoming items, "DROP" to discard items when capacity is reached.</p>
         *
         * @return the backpressure strategy; "BUFFER" by default
         */
        @WithDefault("BUFFER")
        String backpressureStrategy();
    }
}