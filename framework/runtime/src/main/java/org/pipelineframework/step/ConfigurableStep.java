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

package org.pipelineframework.step;

import org.pipelineframework.config.StepConfig;

/**
 * Base class for configurable pipeline steps that use StepConfig
 * to access pipeline configuration.
 */
public abstract class ConfigurableStep implements Configurable {

    StepConfig config = null;


    /**
     * Obtain the effective configuration for this step.
     *
     * @return the current {@code StepConfig} if one has been set; otherwise a new default {@code StepConfig}
     */
    @Override
    public StepConfig effectiveConfig() {
        return config != null ? config : new StepConfig();
    }

    /**
     * Get the configured backpressure buffer capacity for this step.
     *
     * @return the buffer capacity used for backpressure as defined by the effective configuration
     */
    @Override
    public int backpressureBufferCapacity() { return effectiveConfig().backpressureBufferCapacity(); }

    /**
     * Retrieves the backpressure strategy configured for this step.
     *
     * @return the backpressure strategy as a String
     */
    @Override
    public String backpressureStrategy() { return effectiveConfig().backpressureStrategy(); }

    /**
     * Initialise the step with the supplied StepConfig; if the argument is non-null it becomes the step's internal config.
     *
     * Subclasses may override to perform additional or different initialisation behaviour.
     *
     * @param config the configuration to apply to this step; if null the existing internal config is left unchanged
     */
    @Override
    public void initialiseWithConfig(StepConfig config) {
        // Default implementation - subclasses should override if they need specific initialization
        if (config != null) {
            this.config = config; // Update the internal config reference
        }
    }
}