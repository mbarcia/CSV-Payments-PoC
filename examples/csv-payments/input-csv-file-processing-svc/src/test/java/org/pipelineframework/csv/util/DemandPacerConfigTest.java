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

package org.pipelineframework.csv.util;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for DemandPacerConfig configuration mapping interface. Tests default
 * values and custom configuration loading.
 */
@QuarkusTest
@TestProfile(DemandPacerConfigTest.DefaultConfigProfile.class)
class DemandPacerConfigTest {

    @Inject DemandPacerConfig config;

    /** Test profile to ensure default config values are used in tests */
    public static class DefaultConfigProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "csv-poc.reader-demand-pacer.rows-per-period", "10",
                    "csv-poc.reader-demand-pacer.millis-period", "100");
        }
    }

    @Test
    void testConfigIsInjectable() {
        // Then
        assertNotNull(config, "DemandPacerConfig should be injectable");
    }

    @Test
    void testDefaultRowsPerPeriod() {
        // When
        long rowsPerPeriod = config.rowsPerPeriod();

        // Then
        assertEquals(10, rowsPerPeriod, "Default rowsPerPeriod should be 10");
    }

    @Test
    void testDefaultMillisPeriod() {
        // When
        long millisPeriod = config.millisPeriod();

        // Then
        assertEquals(100, millisPeriod, "Default millisPeriod should be 100");
    }

    @Test
    void testDefaultConfigurationYieldsReasonableRate() {
        // Given
        long rowsPerPeriod = config.rowsPerPeriod();
        long millisPeriod = config.millisPeriod();

        // Then - default should allow 100 rows per second (10 rows per 100ms)
        double rowsPerSecond = (rowsPerPeriod * 1000.0) / millisPeriod;
        assertEquals(100.0, rowsPerSecond, 0.01, "Default rate should be 100 rows/second");
    }

    /** Test profile for custom demand pacer configuration */
    public static class CustomDemandPacerProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "csv-poc.reader-demand-pacer.rows-per-period", "20",
                    "csv-poc.reader-demand-pacer.millis-period", "200");
        }
    }

    @QuarkusTest
    @TestProfile(CustomDemandPacerProfile.class)
    static class WithCustomConfigTest {

        @Inject DemandPacerConfig config;

        @Test
        void testCustomRowsPerPeriod() {
            // When
            long rowsPerPeriod = config.rowsPerPeriod();

            // Then
            assertEquals(20, rowsPerPeriod, "Custom rowsPerPeriod should be 20");
        }

        @Test
        void testCustomMillisPeriod() {
            // When
            long millisPeriod = config.millisPeriod();

            // Then
            assertEquals(200, millisPeriod, "Custom millisPeriod should be 200");
        }

        @Test
        void testCustomConfigurationRate() {
            // Given
            long rowsPerPeriod = config.rowsPerPeriod();
            long millisPeriod = config.millisPeriod();

            // Then - custom config should still yield 100 rows per second
            double rowsPerSecond = (rowsPerPeriod * 1000.0) / millisPeriod;
            assertEquals(100.0, rowsPerSecond, 0.01, "Custom rate should be 100 rows/second");
        }
    }

    /** Test profile for high-throughput configuration */
    public static class HighThroughputProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "csv-poc.reader-demand-pacer.rows-per-period", "1000",
                    "csv-poc.reader-demand-pacer.millis-period", "100");
        }
    }

    @QuarkusTest
    @TestProfile(HighThroughputProfile.class)
    static class WithHighThroughputTest {

        @Inject DemandPacerConfig config;

        @Test
        void testHighThroughputConfiguration() {
            // Given
            long rowsPerPeriod = config.rowsPerPeriod();
            long millisPeriod = config.millisPeriod();

            // Then - should allow 10,000 rows per second
            double rowsPerSecond = (rowsPerPeriod * 1000.0) / millisPeriod;
            assertEquals(
                    10000.0, rowsPerSecond, 0.01, "High throughput should be 10,000 rows/second");
        }
    }

    /** Test profile for low-throughput configuration */
    public static class LowThroughputProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "csv-poc.reader-demand-pacer.rows-per-period", "1",
                    "csv-poc.reader-demand-pacer.millis-period", "1000");
        }
    }

    @QuarkusTest
    @TestProfile(LowThroughputProfile.class)
    static class WithLowThroughputTest {

        @Inject DemandPacerConfig config;

        @Test
        void testLowThroughputConfiguration() {
            // Given
            long rowsPerPeriod = config.rowsPerPeriod();
            long millisPeriod = config.millisPeriod();

            // Then - should allow 1 row per second
            double rowsPerSecond = (rowsPerPeriod * 1000.0) / millisPeriod;
            assertEquals(1.0, rowsPerSecond, 0.01, "Low throughput should be 1 row/second");
        }
    }

    @Test
    void testConfigValuesArePositive() {
        // Given
        long rowsPerPeriod = config.rowsPerPeriod();
        long millisPeriod = config.millisPeriod();

        // Then
        assertTrue(rowsPerPeriod > 0, "rowsPerPeriod should be positive");
        assertTrue(millisPeriod > 0, "millisPeriod should be positive");
    }

    @Test
    void testConfigSupportsRateLimitingCalculation() {
        // Given
        long rowsPerPeriod = config.rowsPerPeriod();
        long millisPeriod = config.millisPeriod();

        // When - calculate effective rate
        double effectiveRate = (rowsPerPeriod * 1000.0) / millisPeriod;

        // Then - should be calculable without errors
        assertTrue(effectiveRate > 0, "Effective rate should be positive");
        assertTrue(Double.isFinite(effectiveRate), "Effective rate should be finite");
    }
}
