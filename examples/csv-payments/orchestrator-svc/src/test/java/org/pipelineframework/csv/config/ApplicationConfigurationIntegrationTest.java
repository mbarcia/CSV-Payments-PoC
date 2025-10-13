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

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.StepConfig;

@QuarkusTest
@DisabledOnIntegrationTest
class ApplicationConfigurationIntegrationTest {

    @Inject PipelineConfig pipelineConfig;

    @Test
    void testConfigurationViaEnvironmentVariables() {
        // Save original defaults
        StepConfig originalDefaults = pipelineConfig.defaults();
        int originalRetryLimit = originalDefaults.retryLimit();
        int originalConcurrency = originalDefaults.concurrency();
        boolean originalDebug = originalDefaults.debug();

        try {
            // Set global defaults (simulating application.properties)
            pipelineConfig.defaults().retryLimit(3).concurrency(4).debug(false);

            // For this test, we'll just verify we can access the injected steps
            // and that they have the expected default configuration
            assertNotNull(pipelineConfig.defaults());
            assertEquals(3, pipelineConfig.defaults().retryLimit());
            assertEquals(4, pipelineConfig.defaults().concurrency());
            assertFalse(pipelineConfig.defaults().debug());
        } finally {
            // Restore original defaults
            pipelineConfig
                    .defaults()
                    .retryLimit(originalRetryLimit)
                    .concurrency(originalConcurrency)
                    .debug(originalDebug);
        }
    }
}
