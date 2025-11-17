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
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for PipelineStepConfig configuration mapping interface. Tests the
 * configuration loading from application.properties and default values.
 */
@QuarkusTest
class PipelineStepConfigTest {

    @Inject PipelineStepConfig pipelineStepConfig;

    @Test
    void testDefaultsAreAccessible() {
        // When
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // Then
        assertNotNull(defaults, "Defaults should not be null");
    }

    @Test
    void testDefaultsHaveCorrectDefaultValues() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // Then - verify all default values from @WithDefault annotations
        assertEquals(100, defaults.order(), "Default order should be 100");
        assertEquals(3, defaults.retryLimit(), "Default retryLimit should be 3");
        assertEquals(2000L, defaults.retryWaitMs(), "Default retryWaitMs should be 2000");
        assertFalse(defaults.parallel(), "Default parallel should be false");
        assertFalse(defaults.recoverOnFailure(), "Default recoverOnFailure should be false");
        assertEquals(30000L, defaults.maxBackoff(), "Default maxBackoff should be 30000");
        assertFalse(defaults.jitter(), "Default jitter should be false");
        assertEquals(
                1024,
                defaults.backpressureBufferCapacity(),
                "Default backpressureBufferCapacity should be 1024");
        assertEquals(
                "BUFFER",
                defaults.backpressureStrategy(),
                "Default backpressureStrategy should be BUFFER");
    }

    @Test
    void testStepMapIsAccessible() {
        // When
        Map<String, PipelineStepConfig.StepConfig> stepMap = pipelineStepConfig.step();

        // Then
        assertNotNull(stepMap, "Step map should not be null");
    }

    @Test
    void testStepMapIsEmptyByDefault() {
        // When
        Map<String, PipelineStepConfig.StepConfig> stepMap = pipelineStepConfig.step();

        // Then - without specific configuration, map should be empty
        assertTrue(
                stepMap.isEmpty(), "Step map should be empty without specific step configuration");
    }

    /** Test profile for custom property values */
    public static class CustomDefaultsProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "pipeline.defaults.retry-limit", "10",
                    "pipeline.defaults.retry-wait-ms", "5000",
                    "pipeline.defaults.parallel", "true",
                    "pipeline.defaults.recover-on-failure", "true",
                    "pipeline.defaults.max-backoff", "60000",
                    "pipeline.defaults.jitter", "true",
                    "pipeline.defaults.backpressure-buffer-capacity", "2048",
                    "pipeline.defaults.backpressure-strategy", "DROP");
        }
    }

    @QuarkusTest
    @TestProfile(CustomDefaultsProfile.class)
    static class WithCustomDefaultsTest {

        @Inject PipelineStepConfig pipelineStepConfig;

        @Test
        void testCustomDefaultValues() {
            // Given
            PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

            // Then - verify custom values are loaded
            assertEquals(10, defaults.retryLimit());
            assertEquals(5000L, defaults.retryWaitMs());
            assertTrue(defaults.parallel());
            assertTrue(defaults.recoverOnFailure());
            assertEquals(60000L, defaults.maxBackoff());
            assertTrue(defaults.jitter());
            assertEquals(2048, defaults.backpressureBufferCapacity());
            assertEquals("DROP", defaults.backpressureStrategy());
        }
    }

    /** Test profile for per-step configuration */
    public static class PerStepConfigProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "pipeline.step.\"com.example.MyStep\".order", "50",
                    "pipeline.step.\"com.example.MyStep\".retry-limit", "7",
                    "pipeline.step.\"com.example.MyStep\".parallel", "true",
                    "pipeline.step.\"com.example.AnotherStep\".order", "200",
                    "pipeline.step.\"com.example.AnotherStep\".retry-limit", "15");
        }
    }

    @QuarkusTest
    @TestProfile(PerStepConfigProfile.class)
    static class WithPerStepConfigTest {

        @Inject PipelineStepConfig pipelineStepConfig;

        @Test
        void testPerStepConfigurationIsLoaded() {
            // Given
            Map<String, PipelineStepConfig.StepConfig> stepMap = pipelineStepConfig.step();

            // Then - verify step-specific configuration
            assertNotNull(stepMap);
            assertTrue(
                    stepMap.containsKey("com.example.MyStep"),
                    "Should contain MyStep configuration");
            assertTrue(
                    stepMap.containsKey("com.example.AnotherStep"),
                    "Should contain AnotherStep configuration");

            PipelineStepConfig.StepConfig myStepConfig = stepMap.get("com.example.MyStep");
            assertEquals(50, myStepConfig.order());
            assertEquals(7, myStepConfig.retryLimit());
            assertTrue(myStepConfig.parallel());

            PipelineStepConfig.StepConfig anotherStepConfig =
                    stepMap.get("com.example.AnotherStep");
            assertEquals(200, anotherStepConfig.order());
            assertEquals(15, anotherStepConfig.retryLimit());
        }

        @Test
        void testPerStepConfigInheritsDefaultsForUnsetProperties() {
            // Given
            Map<String, PipelineStepConfig.StepConfig> stepMap = pipelineStepConfig.step();
            PipelineStepConfig.StepConfig myStepConfig = stepMap.get("com.example.MyStep");

            // Then - unset properties should use interface defaults
            assertEquals(2000L, myStepConfig.retryWaitMs(), "Should use default retryWaitMs");
            assertEquals(30000L, myStepConfig.maxBackoff(), "Should use default maxBackoff");
            assertFalse(myStepConfig.jitter(), "Should use default jitter");
            assertEquals(
                    1024,
                    myStepConfig.backpressureBufferCapacity(),
                    "Should use default backpressureBufferCapacity");
            assertEquals(
                    "BUFFER",
                    myStepConfig.backpressureStrategy(),
                    "Should use default backpressureStrategy");
        }
    }

    @Test
    void testStepConfigInterfaceOrderMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Integer order = defaults.order();

        // Then
        assertNotNull(order);
        assertEquals(100, order);
    }

    @Test
    void testStepConfigInterfaceRetryLimitMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Integer retryLimit = defaults.retryLimit();

        // Then
        assertNotNull(retryLimit);
        assertEquals(3, retryLimit);
    }

    @Test
    void testStepConfigInterfaceRetryWaitMsMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Long retryWaitMs = defaults.retryWaitMs();

        // Then
        assertNotNull(retryWaitMs);
        assertEquals(2000L, retryWaitMs);
    }

    @Test
    void testStepConfigInterfaceParallelMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Boolean parallel = defaults.parallel();

        // Then
        assertNotNull(parallel);
        assertFalse(parallel);
    }

    @Test
    void testStepConfigInterfaceRecoverOnFailureMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Boolean recoverOnFailure = defaults.recoverOnFailure();

        // Then
        assertNotNull(recoverOnFailure);
        assertFalse(recoverOnFailure);
    }

    @Test
    void testStepConfigInterfaceMaxBackoffMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Long maxBackoff = defaults.maxBackoff();

        // Then
        assertNotNull(maxBackoff);
        assertEquals(30000L, maxBackoff);
    }

    @Test
    void testStepConfigInterfaceJitterMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Boolean jitter = defaults.jitter();

        // Then
        assertNotNull(jitter);
        assertFalse(jitter);
    }

    @Test
    void testStepConfigInterfaceBackpressureBufferCapacityMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        Integer capacity = defaults.backpressureBufferCapacity();

        // Then
        assertNotNull(capacity);
        assertEquals(1024, capacity);
    }

    @Test
    void testStepConfigInterfaceBackpressureStrategyMethod() {
        // Given
        PipelineStepConfig.StepConfig defaults = pipelineStepConfig.defaults();

        // When
        String strategy = defaults.backpressureStrategy();

        // Then
        assertNotNull(strategy);
        assertEquals("BUFFER", strategy);
    }
}
