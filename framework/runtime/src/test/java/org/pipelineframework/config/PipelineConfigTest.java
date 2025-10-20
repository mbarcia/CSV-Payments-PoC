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

package org.pipelineframework.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PipelineConfigTest {

    @Test
    void testPipelineConfigDefaults() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();

        // When
        StepConfig defaults = pipelineConfig.defaults();

        // Then
        assertNotNull(defaults);
        assertEquals(3, defaults.retryLimit());
        assertFalse(defaults.debug());
        assertFalse(defaults.parallel());
    }

    @Test
    void testPipelineConfigProfileManagement() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();

        // When
        pipelineConfig.profile("test", new StepConfig().retryLimit(5).debug(true).parallel(true));
        pipelineConfig.activate("test");

        StepConfig activeConfig = pipelineConfig.defaults();

        // Then
        assertEquals("test", pipelineConfig.activeProfile());
        assertEquals(5, activeConfig.retryLimit());
        assertTrue(activeConfig.debug());
        assertTrue(activeConfig.parallel());
    }

    @Test
    void testNewStepConfigInheritsDefaults() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.defaults().retryLimit(7).debug(true).parallel(true);

        // When
        StepConfig stepConfig = pipelineConfig.newStepConfig();

        // Then
        assertEquals(7, stepConfig.retryLimit());
        assertTrue(stepConfig.debug());
        assertTrue(stepConfig.parallel());
    }
}
