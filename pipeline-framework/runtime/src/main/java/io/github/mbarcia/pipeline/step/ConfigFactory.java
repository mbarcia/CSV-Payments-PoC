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

package io.github.mbarcia.pipeline.step;

import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.config.LiveStepConfig;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;

@ApplicationScoped
public class ConfigFactory {

    @Inject
    PipelineConfig pipelineConfig;

    public LiveStepConfig buildLiveConfig(Class<?> stepClass, PipelineConfig pipelineConfig) {
        PipelineStep annotation = stepClass.getAnnotation(PipelineStep.class);

        LiveStepConfig config = new LiveStepConfig(Objects.requireNonNullElseGet(pipelineConfig, PipelineConfig::new));

        if (annotation != null) {
            // Apply configuration from annotation
            return (LiveStepConfig) config.overrides()
                    .autoPersist(annotation.autoPersist())
                    .debug(annotation.debug())
                    .recoverOnFailure(annotation.recoverOnFailure())
                    .backpressureBufferCapacity(annotation.backpressureBufferCapacity())
                    .backpressureStrategy(annotation.backpressureStrategy());
        }

        return config;
    }
}