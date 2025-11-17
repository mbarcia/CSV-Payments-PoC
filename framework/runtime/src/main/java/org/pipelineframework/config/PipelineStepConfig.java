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
     * Global default configuration applied to all steps unless overridden
     */
    StepConfig defaults();
    
    /**
     * Per-step configuration overrides
     * Format: pipeline.step."fully.qualified.class.name".property=value
     */
    @WithName("step")
    Map<String, StepConfig> step();
    
    interface StepConfig {
        /**
         * The execution order for this step in the pipeline (optional).
         * <p>
         * Steps are executed in ascending order of this value. Lower numbers execute first.
         * Steps without a specified order default to 100.
         *
         * @return the step order, or 100 if not specified
         */
        @WithDefault("100")
        Integer order();

        /**
         * Maximum number of retry attempts for failed operations.
         * @return maximum retry attempts
         */
        @WithDefault("3")
        Integer retryLimit();

        /**
         * Base delay between retries in milliseconds.
         * @return retry delay in milliseconds
         */
        @WithDefault("2000")
        Long retryWaitMs();

        /**
         * Enable parallel processing.
         * @return true to enable parallel processing, false for sequential processing
         */
        @WithDefault("false")
        Boolean parallel();

        /**
         * Enable failure recovery.
         * @return true to enable recovery, false otherwise
         */
        @WithDefault("false")
        Boolean recoverOnFailure();

        /**
         * Maximum backoff time in milliseconds.
         * @return maximum backoff time in milliseconds
         */
        @WithDefault("30000")
        Long maxBackoff();

        /**
         * Add jitter to retry delays.
         * @return true to add jitter, false otherwise
         */
        @WithDefault("false")
        Boolean jitter();

        /**
         * The backpressure buffer capacity
         * @return the buffer capacity (default: 1024)
         */
        @WithDefault("1024")
        Integer backpressureBufferCapacity();

        /**
         * Backpressure strategy to use when buffering items ("BUFFER", "DROP")
         * @return the backpressure strategy (default: "BUFFER")
         */
        @WithDefault("BUFFER")
        String backpressureStrategy();
    }
}