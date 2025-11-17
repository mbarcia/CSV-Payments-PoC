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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.PipelineStepConfig;
import org.pipelineframework.config.StepConfig;

@ApplicationScoped
public class ConfigFactory {

    @Inject
    PipelineConfig pipelineConfig;

    /**
     * Build a StepConfig for the given step class, applying any per-step overrides to pipeline defaults.
     *
     * Uses the application's PipelineStepConfig to look up a class-specific configuration by the step
     * class's fully qualified name; if found, returns a StepConfig based on the pipeline defaults with
     * those overrides applied, otherwise returns the pipelineConfig's default StepConfig.
     *
     * @param stepClass the step implementation class to resolve per-step overrides for
     * @param pipelineConfig the pipeline-wide configuration providing default step settings
     * @return a StepConfig with per-step overrides applied if available, or the pipeline defaults otherwise
     */
    public StepConfig buildConfig(Class<?> stepClass, PipelineConfig pipelineConfig)
        throws IllegalAccessException {

        // Get the new Quarkus configuration
        PipelineStepConfig pipelineStepConfig = CDI.current()
            .select(PipelineStepConfig.class).get();

        // Get config for the specific class if available
        String className = stepClass.getName();
        PipelineStepConfig.StepConfig classConfig = pipelineStepConfig.step().get(className);

        if (classConfig != null) {
            // Start with pipeline defaults and layer per-step overrides on top
            StepConfig baseConfig = pipelineConfig.newStepConfig();
            
            // Only apply overrides that differ from the default interface values to preserve pipeline defaults
            StepConfig result = baseConfig;
            if (!classConfig.retryLimit().equals(3)) {
                result = result.retryLimit(classConfig.retryLimit());
            }
            if (!classConfig.retryWaitMs().equals(2000L)) {
                result = result.retryWait(java.time.Duration.ofMillis(classConfig.retryWaitMs()));
            }
            if (!classConfig.parallel().equals(false)) {
                result = result.parallel(classConfig.parallel());
            }
            if (!classConfig.recoverOnFailure().equals(false)) {
                result = result.recoverOnFailure(classConfig.recoverOnFailure());
            }
            if (!classConfig.maxBackoff().equals(30000L)) {
                result = result.maxBackoff(java.time.Duration.ofMillis(classConfig.maxBackoff()));
            }
            if (!classConfig.jitter().equals(false)) {
                result = result.jitter(classConfig.jitter());
            }
            if (!classConfig.backpressureBufferCapacity().equals(1024)) {
                result = result.backpressureBufferCapacity(classConfig.backpressureBufferCapacity());
            }
            if (!classConfig.backpressureStrategy().equals("BUFFER")) {
                result = result.backpressureStrategy(classConfig.backpressureStrategy());
            }
            return result;
        } else {
            // Use the PipelineConfig's newStepConfig which contains properly initialized defaults
            return pipelineConfig.newStepConfig();
        }
    }
}