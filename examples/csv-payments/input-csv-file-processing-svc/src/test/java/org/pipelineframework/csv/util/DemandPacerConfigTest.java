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
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DemandPacerConfigTest {

    @Inject
    DemandPacerConfig config;

    @Test
    void testDefaultRowsPerPeriod() {
        // Given/When
        long rowsPerPeriod = config.rowsPerPeriod();

        // Then
        assertTrue(rowsPerPeriod > 0, "Rows per period should be positive");
        assertEquals(15, rowsPerPeriod, "Default rows per period from application.properties should be 15");
    }

    @Test
    void testDefaultMillisPeriod() {
        // Given/When
        long millisPeriod = config.millisPeriod();

        // Then
        assertTrue(millisPeriod > 0, "Millis period should be positive");
        assertEquals(100, millisPeriod, "Default millis period from application.properties should be 100");
    }

    @Test
    void testConfigurationIsInjectable() {
        // Given/When/Then
        assertNotNull(config, "DemandPacerConfig should be injectable via CDI");
    }

    @Test
    void testRowsPerPeriodIsConsistent() {
        // Given
        long firstValue = config.rowsPerPeriod();

        // When
        long secondValue = config.rowsPerPeriod();

        // Then
        assertEquals(firstValue, secondValue, "rowsPerPeriod should return consistent values");
    }

    @Test
    void testMillisPeriodIsConsistent() {
        // Given
        long firstValue = config.millisPeriod();

        // When
        long secondValue = config.millisPeriod();

        // Then
        assertEquals(firstValue, secondValue, "millisPeriod should return consistent values");
    }

    @Test
    void testConfigValuesAreReasonableForRateLimiting() {
        // Given
        long rowsPerPeriod = config.rowsPerPeriod();
        long millisPeriod = config.millisPeriod();

        // Then
        assertTrue(rowsPerPeriod >= 1, "At least 1 row per period is required for rate limiting to make sense");
        assertTrue(millisPeriod >= 1, "Period must be at least 1ms for rate limiting to make sense");
        assertTrue(millisPeriod <= 60000, "Period should be reasonable (not more than 1 minute)");
    }

    @Test
    void testRateLimitingCalculation() {
        // Given
        long rowsPerPeriod = config.rowsPerPeriod();
        long millisPeriod = config.millisPeriod();

        // When - Calculate effective rows per second
        double rowsPerSecond = (rowsPerPeriod * 1000.0) / millisPeriod;

        // Then
        assertTrue(rowsPerSecond > 0, "Calculated rate should be positive");
        assertTrue(rowsPerSecond <= 10000, "Rate should be reasonable (not more than 10k rows/sec)");
    }
}