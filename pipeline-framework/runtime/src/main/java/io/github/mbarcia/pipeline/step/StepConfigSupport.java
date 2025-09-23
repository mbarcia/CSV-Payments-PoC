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
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StepConfigSupport {
    private final LiveStepConfig live;

    public StepConfigSupport(PipelineConfig global, LiveStepConfig live) {
        this.live = live;
        initializeConfigFromAnnotation();
    }

    /**
     * Get the live configuration for this step that can be modified at runtime
     * @return the live step configuration
     */
    public LiveStepConfig liveConfig() {
        return live;
    }
    
    @Inject
    PipelineConfig global;

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
}
