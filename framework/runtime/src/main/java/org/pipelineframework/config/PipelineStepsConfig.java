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
import io.smallrye.config.WithParentName;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration mapping for pipeline steps defined in application properties.
 * <p>
 * This interface uses @ConfigMapping to provide structured, type-safe access
 * to pipeline step configurations defined under the 'pipeline.steps' prefix.
 * It enables compile-time validation and eliminates the need for manual
 * property lookups and unsafe iteration.
 */
@ConfigMapping(prefix = "pipeline.steps")
@Unremovable
public interface PipelineStepsConfig {
    
    /**
     * Get all configured pipeline steps as a map of step names to their configurations.
     * <p>
     * Each entry in the map represents a pipeline step configuration with:
     * - Key: The logical step name (e.g., "process-folder", "send-payment")
     * - Value: A StepConfig object containing the step's properties
     * 
     * @return a map of step names to their configurations
     */
    @WithParentName
    Map<String, StepConfig> steps();
    
    /**
     * Configuration for a single pipeline step.
     * <p>
     * This nested interface defines the structure for individual step configurations
     * with typed access to all supported properties.
     */
    interface StepConfig {
        /**
         * The fully-qualified class name of the step implementation.
         * <p>
         * This property is required and must specify a valid class that implements
         * one of the pipeline step interfaces (StepOneToOne, StepOneToMany, etc.).
         * 
         * @return the step's implementation class name
         */
        String className();
        
        /**
         * The step type identifier (optional).
         * <p>
         * Used to categorize steps and determine their behavior in the pipeline.
         * Common values include "ONE_TO_ONE", "ONE_TO_MANY", "MANY_TO_ONE", "MANY_TO_MANY".
         * 
         * @return the step type, or empty if not specified
         */
        Optional<String> type();
        
        /**
         * The execution order for this step in the pipeline (optional).
         * <p>
         * Steps are executed in ascending order of this value. Lower numbers execute first.
         * Steps without a specified order default to 100.
         * 
         * @return the step order, or 100 if not specified
         */
        @WithDefault("100")
        int order();
        
        /**
         * Whether to enable parallel processing for this step (optional).
         * <p>
         * When true, enables concurrent processing of items within this step.
         * When false (default), maintains sequential processing for backward compatibility.
         * 
         * @return true to enable parallel processing, false for sequential processing
         */
        @WithDefault("false")
        boolean parallel();
    }
}