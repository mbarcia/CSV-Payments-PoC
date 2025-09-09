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

package io.github.mbarcia.pipeline.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LiveStepConfigTest {

  @Test
  void testLiveStepConfigInheritsFromPipelineDefaults() {
    // Given
    PipelineConfig pipelineConfig = new PipelineConfig();
    pipelineConfig.defaults().retryLimit(10).retryWait(Duration.ofSeconds(2));

    // When
    LiveStepConfig liveConfig = new LiveStepConfig(pipelineConfig);

    // Then
    assertEquals(10, liveConfig.retryLimit());
    assertEquals(Duration.ofSeconds(2), liveConfig.retryWait());
  }

  @Test
  void testLiveStepConfigOverrides() {
    // Given
    PipelineConfig pipelineConfig = new PipelineConfig();
    pipelineConfig.defaults().retryLimit(5).retryWait(Duration.ofSeconds(1));

    LiveStepConfig liveConfig = new LiveStepConfig(pipelineConfig);

    // When
    liveConfig.overrides().retryLimit(15).retryWait(Duration.ofSeconds(3));

    // Then
    assertEquals(15, liveConfig.retryLimit());
    assertEquals(Duration.ofSeconds(3), liveConfig.retryWait());
  }

  @Test
  void testLiveStepConfigUsesDefaultsWhenNoOverrides() {
    // Given
    PipelineConfig pipelineConfig = new PipelineConfig();
    pipelineConfig.defaults().retryLimit(8).debug(true);

    LiveStepConfig liveConfig = new LiveStepConfig(pipelineConfig);
    // Not setting any overrides

    // When & Then
    assertEquals(8, liveConfig.retryLimit());
    assertTrue(liveConfig.debug());
  }
}
