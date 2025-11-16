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

import java.time.Duration;
import org.pipelineframework.config.StepConfig;

public interface Configurable {
    /**
     * Get the effective configuration for this step.
     *
     * @return the step configuration
     */
    StepConfig effectiveConfig();

    // Default configuration accessors
    default int retryLimit() { return effectiveConfig().retryLimit(); }
    default Duration retryWait() { return effectiveConfig().retryWait(); }
    default boolean recoverOnFailure() { return effectiveConfig().recoverOnFailure(); }
    default Duration maxBackoff() { return effectiveConfig().maxBackoff(); }
    default boolean jitter() { return effectiveConfig().jitter(); }
    default int backpressureBufferCapacity() { return effectiveConfig().backpressureBufferCapacity(); }
    default String backpressureStrategy() { return effectiveConfig().backpressureStrategy(); }
    default boolean parallel() { return effectiveConfig().parallel(); }

    void initialiseWithConfig(StepConfig config);
}