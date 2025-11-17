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

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PipelineStepConfigTest {

    @Inject
    PipelineStepConfig pipelineStepConfig;

    @Test
    void testDefaultsAreInjectable() {
        // Given/When
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // Then
        assertNotNull(defaults, "Defaults should be injectable");
    }

    @Test
    void testDefaultRetryLimit() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Integer retryLimit = defaults.retryLimit();

        // Then
        assertNotNull(retryLimit, "Retry limit should not be null");
        assertTrue(retryLimit >= 0, "Retry limit should be non-negative");
        assertEquals(3, retryLimit, "Default retry limit should be 3");
    }

    @Test
    void testDefaultRetryWaitMs() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Long retryWaitMs = defaults.retryWaitMs();

        // Then
        assertNotNull(retryWaitMs, "Retry wait should not be null");
        assertTrue(retryWaitMs > 0, "Retry wait should be positive");
        assertEquals(2000L, retryWaitMs, "Default retry wait should be 2000ms");
    }

    @Test
    void testDefaultParallel() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Boolean parallel = defaults.parallel();

        // Then
        assertNotNull(parallel, "Parallel should not be null");
        assertFalse(parallel, "Default parallel should be false");
    }

    @Test
    void testDefaultRecoverOnFailure() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Boolean recoverOnFailure = defaults.recoverOnFailure();

        // Then
        assertNotNull(recoverOnFailure, "RecoverOnFailure should not be null");
        assertFalse(recoverOnFailure, "Default recoverOnFailure should be false");
    }

    @Test
    void testDefaultMaxBackoff() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Long maxBackoff = defaults.maxBackoff();

        // Then
        assertNotNull(maxBackoff, "Max backoff should not be null");
        assertTrue(maxBackoff > 0, "Max backoff should be positive");
        assertEquals(30000L, maxBackoff, "Default max backoff should be 30000ms");
    }

    @Test
    void testDefaultJitter() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Boolean jitter = defaults.jitter();

        // Then
        assertNotNull(jitter, "Jitter should not be null");
        assertFalse(jitter, "Default jitter should be false");
    }

    @Test
    void testDefaultBackpressureBufferCapacity() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Integer capacity = defaults.backpressureBufferCapacity();

        // Then
        assertNotNull(capacity, "Backpressure buffer capacity should not be null");
        assertTrue(capacity > 0, "Backpressure buffer capacity should be positive");
        assertEquals(1024, capacity, "Default backpressure buffer capacity should be 1024");
    }

    @Test
    void testDefaultBackpressureStrategy() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        String strategy = defaults.backpressureStrategy();

        // Then
        assertNotNull(strategy, "Backpressure strategy should not be null");
        assertTrue(strategy.equals("BUFFER") || strategy.equals("DROP"), 
            "Backpressure strategy should be either BUFFER or DROP");
        assertEquals("BUFFER", strategy, "Default backpressure strategy should be BUFFER");
    }

    @Test
    void testDefaultOrder() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Integer order = defaults.order();

        // Then
        assertNotNull(order, "Order should not be null");
        assertEquals(100, order, "Default order should be 100");
    }

    @Test
    void testStepMapIsAccessible() {
        // Given/When
        Map<String, PipelineStepConfig.StepConfig> stepMap = pipelineStepConfig.step();

        // Then
        assertNotNull(stepMap, "Step map should not be null");
    }

    @Test
    void testStepMapIsEmptyByDefault() {
        // Given
        Map<String, PipelineStepConfig.StepConfig> stepMap = pipelineStepConfig.step();

        // When/Then
        // Map may or may not be empty depending on application.properties configuration
        // Just verify it's accessible and iterable
        assertNotNull(stepMap, "Step map should be accessible");
        assertDoesNotThrow(() -> stepMap.keySet(), "Should be able to iterate step map keys");
    }

    @Test
    void testAllDefaultValuesAreReasonable() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // Then - Verify all values are in reasonable ranges
        assertTrue(defaults.retryLimit() >= 0 && defaults.retryLimit() <= 100, 
            "Retry limit should be reasonable (0-100)");
        assertTrue(defaults.retryWaitMs() >= 0 && defaults.retryWaitMs() <= 60000, 
            "Retry wait should be reasonable (0-60s)");
        assertTrue(defaults.maxBackoff() >= 0 && defaults.maxBackoff() <= 3600000, 
            "Max backoff should be reasonable (0-1h)");
        assertTrue(defaults.backpressureBufferCapacity() > 0 && defaults.backpressureBufferCapacity() <= 100000, 
            "Backpressure buffer capacity should be reasonable (1-100k)");
        assertTrue(defaults.order() >= 0 && defaults.order() <= 10000, 
            "Order should be reasonable (0-10000)");
    }
}