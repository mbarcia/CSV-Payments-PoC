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

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Objects;
import org.pipelineframework.annotation.PipelineStep;
import org.pipelineframework.config.LiveStepConfig;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.StepConfig;

@ApplicationScoped
public class ConfigFactory {

    @Inject
    PipelineConfig pipelineConfig;

    public LiveStepConfig buildLiveConfig(Class<?> stepClass, PipelineConfig pipelineConfig)
        throws IllegalAccessException {

        StepConfig overrides = new StepConfig();
	    try {
            Field originalServiceClassField = stepClass.getField("ORIGINAL_SERVICE_CLASS");
            Class<?> originalServiceClass =  (Class<?>) originalServiceClassField.get(null);
            PipelineStep annotation = originalServiceClass.getAnnotation(PipelineStep.class);
            if (annotation == null) {
                throw new RuntimeException("Null annotation, when a pipeline step must have come from an annotation");
            }
            overrides
                    .autoPersist(annotation.autoPersist())
                    .recoverOnFailure(annotation.recoverOnFailure())
                    .backpressureBufferCapacity(annotation.backpressureBufferCapacity())
                    .backpressureStrategy(annotation.backpressureStrategy())
                    .batchSize(annotation.batchSize())
                    .batchTimeout(Duration.ofMillis(annotation.batchTimeoutMs()))
                    .debug(annotation.debug())
                    .parallel(annotation.parallel());
	    } catch (NoSuchFieldException e) {
          // Test classes usually do not have this field
          Log.warnf("Field ORIGINAL_SERVICE_CLASS not found, make sure this is a test run. Continuing...");
	    }

        return new LiveStepConfig(overrides, Objects.requireNonNullElseGet(pipelineConfig, PipelineConfig::new));
    }
}