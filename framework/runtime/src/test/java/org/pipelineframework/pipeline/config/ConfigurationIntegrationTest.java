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

package org.pipelineframework.pipeline.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.StepConfig;

class ConfigurationIntegrationTest {

    @Test
    void testPipelineConfigDefaults() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        StepConfig defaults = pipelineConfig.defaults();

        // Then
        assertNotNull(defaults);
        assertEquals(3, defaults.retryLimit());
        assertEquals(Duration.ofMillis(2000), defaults.retryWait());
        assertFalse(defaults.parallel());
        assertFalse(defaults.debug());
        assertFalse(defaults.recoverOnFailure());
        assertFalse(defaults.runWithVirtualThreads());
        assertEquals(Duration.ofSeconds(30), defaults.maxBackoff());
        assertFalse(defaults.jitter());
    }

    @Test
    void testStepSpecificConfiguration() {
        // Given
        StepConfig config = new StepConfig();

        // When
        config.retryLimit(10).parallel(true).debug(true);

        // Then
        assertEquals(10, config.retryLimit());
        assertTrue(config.parallel());
        assertTrue(config.debug());
        // Other properties should still use defaults
        assertEquals(Duration.ofMillis(2000), config.retryWait());
        assertFalse(config.recoverOnFailure());
    }

    @Test
    void testProfileBasedConfiguration() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.profile(
                "test",
                new StepConfig().retryLimit(5).retryWait(Duration.ofSeconds(1)).debug(true));

        // When
        pipelineConfig.activate("test");
        StepConfig activeConfig = pipelineConfig.defaults();

        // Then
        assertEquals(5, activeConfig.retryLimit());
        assertEquals(Duration.ofSeconds(1), activeConfig.retryWait());
        assertTrue(activeConfig.debug());
    }

    @Test
    void testProfileConfiguration() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.profile("custom", new StepConfig().retryLimit(7).parallel(true));
        pipelineConfig.activate("custom");

        // When
        StepConfig activeConfig = pipelineConfig.defaults();

        // Then
        assertEquals(7, activeConfig.retryLimit()); // from profile
        assertTrue(activeConfig.parallel()); // from profile
        assertEquals(Duration.ofMillis(2000), activeConfig.retryWait()); // default
    }
}
