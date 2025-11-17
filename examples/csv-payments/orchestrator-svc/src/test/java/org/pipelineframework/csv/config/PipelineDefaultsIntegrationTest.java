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

package org.pipelineframework.csv.config;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.PipelineStepConfig;
import org.pipelineframework.config.StepConfig;

@QuarkusTest
class PipelineDefaultsIntegrationTest {

    @Inject PipelineConfig pipelineConfig;

    @Inject PipelineStepConfig pipelineStepConfig;

    @Test
    void testNewStepConfigInheritsDefaults() {
        // Verify that newStepConfig() inherits all default values from defaults()
        StepConfig defaults = pipelineConfig.defaults();
        StepConfig newStepConfig = pipelineConfig.newStepConfig();

        // Verify inheritance between newStepConfig and defaults
        assertEquals(defaults.retryLimit(), newStepConfig.retryLimit());
        assertEquals(defaults.retryWait(), newStepConfig.retryWait());
        assertEquals(defaults.parallel(), newStepConfig.parallel());
        assertEquals(defaults.recoverOnFailure(), newStepConfig.recoverOnFailure());
        assertEquals(defaults.maxBackoff(), newStepConfig.maxBackoff());
        assertEquals(defaults.jitter(), newStepConfig.jitter());
        assertEquals(
                defaults.backpressureBufferCapacity(), newStepConfig.backpressureBufferCapacity());
        assertEquals(defaults.backpressureStrategy(), newStepConfig.backpressureStrategy());
    }
}
