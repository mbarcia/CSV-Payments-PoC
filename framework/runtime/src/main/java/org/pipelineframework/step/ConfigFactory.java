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

    public StepConfig buildConfig(Class<?> stepClass, PipelineConfig pipelineConfig)
        throws IllegalAccessException {

        // Get the new Quarkus configuration
        PipelineStepConfig pipelineStepConfig = CDI.current()
            .select(PipelineStepConfig.class).get();

        // Get config for the specific class if available
        String className = stepClass.getName();
        PipelineStepConfig.StepConfig classConfig = pipelineStepConfig.step().get(className);

        if (classConfig != null) {
            // Use specific configuration for this step class
            return new StepConfig(classConfig);
        } else {
            // Use the PipelineConfig's newStepConfig which contains properly initialized defaults
            return pipelineConfig.newStepConfig();
        }
    }
}