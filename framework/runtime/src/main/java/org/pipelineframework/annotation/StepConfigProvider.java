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

package org.pipelineframework.annotation;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.step.ConfigurableStep;

/**
 * Base class for adapters that can automatically access step configuration.
 */
public class StepConfigProvider {
    
    private static final Map<Class<?>, StepConfig> configCache = new ConcurrentHashMap<>();
    
    /**
     * Get the step configuration for a given step class.
     * 
     * @param stepClass the step class
     * @return the step configuration
     */
    public static StepConfig getStepConfig(Class<? extends ConfigurableStep> stepClass) {
        return configCache.computeIfAbsent(stepClass, cls -> {
            // First, try to get the configuration from the annotation
            PipelineStep annotation = cls.getAnnotation(PipelineStep.class);
            
            if (annotation != null) {
                // Build configuration from the annotation
                StepConfig config = new StepConfig();
                config
                    .autoPersist(annotation.autoPersist())
                    .debug(annotation.debug())
                    .recoverOnFailure(annotation.recoverOnFailure())
                    .backpressureBufferCapacity(annotation.backpressureBufferCapacity())
                    .backpressureStrategy(annotation.backpressureStrategy())
                    .batchSize(annotation.batchSize())
                    .batchTimeout(Duration.ofMillis(annotation.batchTimeoutMs()))
                    .parallel(annotation.parallel());
                return config;
            } else {
                // If no annotation, return a default config
                return new StepConfig();
            }
        });
    }
    
    /**
     * Check if auto-persistence is enabled for a given step class.
     * 
     * @param stepClass the step class
     * @return true if auto-persistence is enabled, false otherwise
     */
    public static boolean isAutoPersistenceEnabled(Class<? extends ConfigurableStep> stepClass) {
        StepConfig config = getStepConfig(stepClass);
        return config != null && config.autoPersist();
    }
}