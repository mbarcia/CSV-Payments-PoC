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

package io.github.mbarcia.pipeline.step;

import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.config.LiveStepConfig;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.config.StepConfig;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import java.util.Objects;

/**
 * Base class for configurable pipeline steps that use LiveStepConfig
 * to dynamically access pipeline configuration.
 * 
 * This class implements StepOneToMany as a default, but subclasses can override
 * to implement a different cardinality.
 */
public abstract class ConfigurableStep implements StepOneToMany<Object, Object> {
    
    @Inject
    PipelineConfig pipelineConfig;
    
    private LiveStepConfig config;

    protected ConfigurableStep() {
        // Config will be set by CDI injection
        initializeConfigFromAnnotation();
    }

    @Override
    public StepConfig effectiveConfig() {
        if (config == null) {
            // Fallback to a default config if pipelineConfig is not injected yet
            // This shouldn't happen in normal operation, but prevents NPEs
            config = new LiveStepConfig(Objects.requireNonNullElseGet(pipelineConfig, PipelineConfig::new));
        }
        return config;
    }

    /**
     * Get the live configuration for this step that can be modified at runtime
     * @return the live step configuration
     */
    public LiveStepConfig liveConfig() {
        return (LiveStepConfig) effectiveConfig();
    }
    
    /**
     * Initialize configuration from @PipelineStep annotation if present
     */
    private void initializeConfigFromAnnotation() {
        Class<?> stepClass = this.getClass();
        PipelineStep annotation = stepClass.getAnnotation(PipelineStep.class);
        
        if (annotation != null) {
            // Apply configuration from annotation
            liveConfig().overrides()
                .autoPersist(annotation.autoPersist())
                .debug(annotation.debug())
                .recoverOnFailure(annotation.recoverOnFailure());
        }
    }
    
    /**
     * Subclasses should override this method to provide the actual transformation logic.
     * 
     * @param input The input item
     * @return A Multi that emits the transformed output items
     */
    @Override
    public Multi<Object> applyMulti(Object input) {
        // This should be overridden by subclasses
        throw new UnsupportedOperationException("Subclasses must override applyMulti method");
    }
}