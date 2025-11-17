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

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.SmallRyeConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.PipelineStepConfig;
import org.pipelineframework.config.StepConfig;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PipelineDefaultsIntegrationTest {

    @Inject
    PipelineConfig pipelineConfig;

    @Inject
    PipelineStepConfig pipelineStepConfig;

    @Test
    void testPipelineConfigDefaultsAreSyncedWithPipelineStepConfig() {
        // Get the defaults from the configuration system (PipelineStepConfig)
        PipelineStepConfig.StepConfig configFromStepConfig = pipelineStepConfig.defaults();

        // Get the defaults from PipelineConfig
        StepConfig configFromPipelineConfig = pipelineConfig.defaults();

        System.out.println("PipelineStepConfig.retryWaitMs(): " + configFromStepConfig.retryWaitMs());
        System.out.println("PipelineConfig.retryWait().toMillis(): " + configFromPipelineConfig.retryWait().toMillis());

        // Verify that both configurations have the same values
        // This proves that the PipelineConfigInitializer is correctly
        // synchronizing values from PipelineStepConfig to PipelineConfig
        assertEquals(configFromStepConfig.retryLimit(), configFromPipelineConfig.retryLimit(),
                "PipelineConfig should have the same retryLimit as PipelineStepConfig");
        assertEquals(configFromStepConfig.retryWaitMs(), configFromPipelineConfig.retryWait().toMillis(),
                "PipelineConfig should have the same retryWait as PipelineStepConfig");
        assertEquals(configFromStepConfig.parallel(), configFromPipelineConfig.parallel(),
                "PipelineConfig should have the same parallel setting as PipelineStepConfig");
        assertEquals(configFromStepConfig.recoverOnFailure(), configFromPipelineConfig.recoverOnFailure(),
                "PipelineConfig should have the same recoverOnFailure setting as PipelineStepConfig");
        assertEquals(configFromStepConfig.maxBackoff(), configFromPipelineConfig.maxBackoff().toMillis(),
                "PipelineConfig should have the same maxBackoff as PipelineStepConfig");
        assertEquals(configFromStepConfig.jitter(), configFromPipelineConfig.jitter(),
                "PipelineConfig should have the same jitter setting as PipelineStepConfig");
        assertEquals(configFromStepConfig.backpressureBufferCapacity(), configFromPipelineConfig.backpressureBufferCapacity(),
                "PipelineConfig should have the same backpressureBufferCapacity as PipelineStepConfig");
        assertEquals(configFromStepConfig.backpressureStrategy(), configFromPipelineConfig.backpressureStrategy(),
                "PipelineConfig should have the same backpressureStrategy as PipelineStepConfig");
    }

    @Test
    void testPipelineConfigNewStepConfigInheritsDefaults() {
        // Test that when creating new StepConfig instances via newStepConfig(),
        // they inherit from the properly synced defaults
        StepConfig newStepConfig = pipelineConfig.newStepConfig();
        StepConfig defaults = pipelineConfig.defaults();
        
        // Verify inheritance
        assertEquals(defaults.retryLimit(), newStepConfig.retryLimit());
        assertEquals(defaults.retryWait(), newStepConfig.retryWait());
        assertEquals(defaults.parallel(), newStepConfig.parallel());
        assertEquals(defaults.recoverOnFailure(), newStepConfig.recoverOnFailure());
        assertEquals(defaults.maxBackoff(), newStepConfig.maxBackoff());
        assertEquals(defaults.jitter(), newStepConfig.jitter());
        assertEquals(defaults.backpressureBufferCapacity(), newStepConfig.backpressureBufferCapacity());
        assertEquals(defaults.backpressureStrategy(), newStepConfig.backpressureStrategy());
    }
}