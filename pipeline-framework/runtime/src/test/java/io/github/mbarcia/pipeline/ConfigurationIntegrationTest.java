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

package io.github.mbarcia.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConfigurationIntegrationTest {

  static class ConfigTestStepBlocking extends ConfigurableStep
      implements StepOneToOneBlocking<String, String> {
    @Override
    public String apply(String input) {
      return "ConfigTest: " + input;
    }
  }

  @Test
  void testPipelineConfigDefaults() {
    try (PipelineRunner runner = new PipelineRunner()) {
      PipelineConfig pipelineConfig = new PipelineConfig();

      // Check default values
      StepConfig defaults = pipelineConfig.defaults();
      assertEquals(3, defaults.retryLimit());
      assertEquals(Duration.ofMillis(200), defaults.retryWait());
      assertEquals(4, defaults.concurrency());
      assertFalse(defaults.debug());
      assertFalse(defaults.recoverOnFailure());
      assertFalse(defaults.runWithVirtualThreads());
      assertEquals(Duration.ofSeconds(30), defaults.maxBackoff());
      assertFalse(defaults.jitter());
    }
  }

  @Test
  void testPipelineConfigProfileManagement() {
    try (PipelineRunner runner = new PipelineRunner()) {
      PipelineConfig pipelineConfig = new PipelineConfig();

      // Set up profiles
      pipelineConfig.profile("test", new StepConfig().retryLimit(5).debug(true));
      pipelineConfig.activate("test");

      // Verify active profile
      assertEquals("test", pipelineConfig.activeProfile());

      StepConfig activeConfig = pipelineConfig.defaults();
      assertEquals(5, activeConfig.retryLimit());
      assertTrue(activeConfig.debug());
    }
  }

  @Test
  void testNewStepConfigInheritsDefaults() {
    try (PipelineRunner runner = new PipelineRunner()) {
      PipelineConfig pipelineConfig = new PipelineConfig();
      pipelineConfig.defaults().retryLimit(8).debug(true);

      StepConfig stepConfig = pipelineConfig.newStepConfig();

      assertEquals(8, stepConfig.retryLimit());
      assertTrue(stepConfig.debug());
    }
  }
}
