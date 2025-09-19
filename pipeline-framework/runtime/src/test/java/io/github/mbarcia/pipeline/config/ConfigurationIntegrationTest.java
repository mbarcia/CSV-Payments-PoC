/*
 * Copyright © 2023-2025 Mariano Barcia
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

package io.github.mbarcia.pipeline.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConfigurationIntegrationTest {

  @Test
  void testPipelineConfigDefaults() {
    // Given
    PipelineConfig pipelineConfig = new PipelineConfig();
    StepConfig defaults = pipelineConfig.defaults();

    // Then
    assertNotNull(defaults);
    assertEquals(3, defaults.retryLimit());
    assertEquals(Duration.ofMillis(200), defaults.retryWait());
    assertEquals(4, defaults.concurrency());
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
    config.retryLimit(10).concurrency(20).debug(true);

    // Then
    assertEquals(10, config.retryLimit());
    assertEquals(20, config.concurrency());
    assertTrue(config.debug());
    // Other properties should still use defaults
    assertEquals(Duration.ofMillis(200), config.retryWait());
    assertFalse(config.recoverOnFailure());
  }

  @Test
  void testProfileBasedConfiguration() {
    // Given
    PipelineConfig pipelineConfig = new PipelineConfig();
    pipelineConfig.profile(
        "test", new StepConfig().retryLimit(5).retryWait(Duration.ofSeconds(1)).debug(true));

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
    pipelineConfig.profile("custom", new StepConfig().retryLimit(7).concurrency(15));
    pipelineConfig.activate("custom");

    // When
    StepConfig activeConfig = pipelineConfig.defaults();

    // Then
    assertEquals(7, activeConfig.retryLimit()); // from profile
    assertEquals(15, activeConfig.concurrency()); // from profile
    assertEquals(Duration.ofMillis(200), activeConfig.retryWait()); // default
  }
}
