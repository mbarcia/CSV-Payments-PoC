/*
 * Copyright © 2023-2025 Mariano Barcia
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

package io.github.mbarcia.pipeline.service;

import io.smallrye.mutiny.Uni;

/**
 * Generic step supplier that can handle different input/output cardinalities.
 * This interface is the base for all step suppliers in the pipeline.
 * 
 * @param <IN> Input type
 * @param <OUT> Output type
 */
public interface PipelineStep<IN, OUT> {
    
    /**
     * Execute the step with the given input.
     * 
     * @param input The input for this step
     * @return Uni with the output of this step
     */
    Uni<OUT> execute(IN input);
    
    /**
     * Get the execution configuration for this step.
     * 
     * @return The execution configuration
     */
    default StepExecutionConfig getExecutionConfig() {
        return StepExecutionConfig.DEFAULT;
    }
}