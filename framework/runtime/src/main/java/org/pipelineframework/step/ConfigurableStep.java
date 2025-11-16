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


    @Override
    public StepConfig effectiveConfig() {
        return config != null ? config : new StepConfig();
    }

    @Override
    public int backpressureBufferCapacity() { return effectiveConfig().backpressureBufferCapacity(); }

    @Override
    public String backpressureStrategy() { return effectiveConfig().backpressureStrategy(); }

    @Override
    public void initialiseWithConfig(StepConfig config) {
        // Default implementation - subclasses should override if they need specific initialization
        if (config != null) {
            this.config = config; // Update the internal config reference
        }
    }
}