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

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LiveStepConfigTest {

    @Test
    void testLiveStepConfigInheritsFromPipelineDefaults() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig
                .defaults()
                .retryLimit(10)
                .retryWait(Duration.ofSeconds(2))
                .parallel(true)
                .debug(true)
                .recoverOnFailure(true)
                .runWithVirtualThreads(true)
                .maxBackoff(Duration.ofMinutes(1))
                .jitter(true);

        // When
        LiveStepConfig liveConfig = new LiveStepConfig(new StepConfig(), pipelineConfig);

        // Then
        assertEquals(10, liveConfig.retryLimit());
        assertEquals(Duration.ofSeconds(2), liveConfig.retryWait());
        assertTrue(liveConfig.parallel());
        assertTrue(liveConfig.debug());
        assertTrue(liveConfig.recoverOnFailure());
        assertTrue(liveConfig.runWithVirtualThreads());
        assertEquals(Duration.ofMinutes(1), liveConfig.maxBackoff());
        assertTrue(liveConfig.jitter());
    }

    @Test
    void testLiveStepConfigOverrides() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig
                .defaults()
                .retryLimit(5)
                .retryWait(Duration.ofSeconds(1))
                .parallel(false)
                .debug(false)
                .recoverOnFailure(false)
                .runWithVirtualThreads(false)
                .maxBackoff(Duration.ofSeconds(30))
                .jitter(false);

        StepConfig overrides = new StepConfig();
        LiveStepConfig liveConfig = new LiveStepConfig(overrides, pipelineConfig);

        // When
        overrides
                .retryLimit(15)
                .retryWait(Duration.ofSeconds(3))
                .parallel(true)
                .debug(true)
                .recoverOnFailure(true)
                .runWithVirtualThreads(true)
                .maxBackoff(Duration.ofMinutes(2))
                .jitter(true);

        // Then
        assertEquals(15, liveConfig.retryLimit());
        assertEquals(Duration.ofSeconds(3), liveConfig.retryWait());
        assertTrue(liveConfig.parallel());
        assertTrue(liveConfig.debug());
        assertTrue(liveConfig.recoverOnFailure());
        assertTrue(liveConfig.runWithVirtualThreads());
        assertEquals(Duration.ofMinutes(2), liveConfig.maxBackoff());
        assertTrue(liveConfig.jitter());
    }

    @Test
    void testLiveStepConfigUsesDefaultsWhenNoOverrides() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.defaults().retryLimit(8).debug(true).parallel(true).recoverOnFailure(true);

        StepConfig overrides = new StepConfig();
        LiveStepConfig liveConfig = new LiveStepConfig(overrides, pipelineConfig);
        // Not setting any overrides

        // When & Then
        assertEquals(8, liveConfig.retryLimit());
        assertTrue(liveConfig.debug());
        assertTrue(liveConfig.parallel()); // from defaults
        assertTrue(liveConfig.recoverOnFailure());
    }

    @Test
    void testLiveStepConfigPartialOverrides() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig
                .defaults()
                .retryLimit(5)
                .retryWait(Duration.ofSeconds(1))
                .parallel(false)
                .debug(false)
                .recoverOnFailure(false);

        StepConfig overrides = new StepConfig();
        LiveStepConfig liveConfig = new LiveStepConfig(overrides, pipelineConfig);

        // When - only override some properties
        overrides.retryLimit(15).debug(true);
        // parallel, recoverOnFailure, etc. are not overridden

        // Then - overridden values should be used, others should fall back to defaults
        assertEquals(15, liveConfig.retryLimit()); // overridden
        assertEquals(Duration.ofSeconds(1), liveConfig.retryWait()); // default
        assertFalse(liveConfig.parallel()); // default
        assertTrue(liveConfig.debug()); // overridden
        assertFalse(liveConfig.recoverOnFailure()); // default
    }

    @Test
    void testLiveStepConfigOverrideWithSameValuesAsDefaults() {
        // Given
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.defaults().retryLimit(5).debug(false);

        StepConfig overrides = new StepConfig();
        LiveStepConfig liveConfig = new LiveStepConfig(overrides, pipelineConfig);

        // When - override with the same values as defaults
        overrides
                .retryLimit(5) // same as default
                .debug(false); // same as default

        // Then - should still use the override values
        assertEquals(5, liveConfig.retryLimit());
        assertFalse(liveConfig.debug());
    }
}
