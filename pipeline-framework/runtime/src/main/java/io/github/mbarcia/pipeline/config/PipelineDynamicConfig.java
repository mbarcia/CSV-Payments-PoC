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

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dynamic configuration holder for pipeline settings.
 * Allows runtime updates of pipeline configuration.
 * <p>
 * This follows the pattern of using atomic references for thread-safe
 * runtime configuration updates without breaking FunctionalInterface contracts.
 */
@ApplicationScoped
public class PipelineDynamicConfig {
    
    private final AtomicReference<PipelineConfigValues> currentConfig = 
        new AtomicReference<>(new PipelineConfigValues(1000, 3, 1000L));

    /**
     * Update the configuration values at runtime.
     * @param concurrency new concurrency limit
     * @param retryLimit new maximum retry attempts
     * @param retryWaitMs new initial retry delay in milliseconds
     */
    public void updateConfig(int concurrency, int retryLimit, long retryWaitMs) {
        currentConfig.set(new PipelineConfigValues(concurrency, retryLimit, retryWaitMs));
    }

    /**
     * Update the configuration from the static config interface.
     * @param staticConfig the static configuration to copy values from
     */
    public void updateConfig(PipelineInitialConfig staticConfig) {
        updateConfig(
            staticConfig.concurrency(), 
            staticConfig.retryLimit(), 
            staticConfig.retryWaitMs()
        );
    }

    /**
     * Immutable holder for configuration values.
     */
    private static class PipelineConfigValues {
        final int concurrency;
        final int retryLimit;
        final long retryWaitMs;

        PipelineConfigValues(int concurrency, int retryLimit, long retryWaitMs) {
            this.concurrency = concurrency;
            this.retryLimit = retryLimit;
            this.retryWaitMs = retryWaitMs;
        }
    }
    
    // Getter methods for all the configuration properties
    public int getRetryLimit() {
        return currentConfig.get().retryLimit;
    }
    
    public Duration getRetryWait() {
        return Duration.ofMillis(currentConfig.get().retryWaitMs);
    }
    
    public int getConcurrency() {
        return currentConfig.get().concurrency;
    }
    
    public boolean isDebug() {
        return false; // Default value
    }
    
    public boolean isRecoverOnFailure() {
        return false; // Default value
    }
    
    public boolean isRunWithVirtualThreads() {
        return false; // Default value
    }
    
    public Duration getMaxBackoff() {
        return Duration.ofSeconds(30); // Default value
    }
    
    public boolean isJitter() {
        return false; // Default value
    }
    
    public boolean isAutoPersist() {
        return true; // Default value - auto-persistence enabled by default
    }
}