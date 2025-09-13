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
     * Get the current concurrency limit for records processing.
     * @return concurrency limit
     */
    public int getConcurrencyLimitRecords() {
        return currentConfig.get().concurrencyLimitRecords;
    }

    /**
     * Get the current maximum number of retry attempts.
     * @return maximum retry attempts
     */
    public int getMaxRetries() {
        return currentConfig.get().maxRetries;
    }

    /**
     * Get the current initial retry delay in milliseconds.
     * @return initial retry delay in milliseconds
     */
    public long getInitialRetryDelay() {
        return currentConfig.get().initialRetryDelay;
    }

    /**
     * Update the configuration values at runtime.
     * @param concurrencyLimitRecords new concurrency limit
     * @param maxRetries new maximum retry attempts
     * @param initialRetryDelay new initial retry delay in milliseconds
     */
    public void updateConfig(int concurrencyLimitRecords, int maxRetries, long initialRetryDelay) {
        currentConfig.set(new PipelineConfigValues(concurrencyLimitRecords, maxRetries, initialRetryDelay));
    }

    /**
     * Update the configuration from the static config interface.
     * @param staticConfig the static configuration to copy values from
     */
    public void updateConfig(PipelineInitialConfig staticConfig) {
        updateConfig(
            staticConfig.concurrencyLimitRecords(), 
            staticConfig.maxRetries(), 
            staticConfig.initialRetryDelay()
        );
    }

    /**
     * Immutable holder for configuration values.
     */
    private static class PipelineConfigValues {
        final int concurrencyLimitRecords;
        final int maxRetries;
        final long initialRetryDelay;

        PipelineConfigValues(int concurrencyLimitRecords, int maxRetries, long initialRetryDelay) {
            this.concurrencyLimitRecords = concurrencyLimitRecords;
            this.maxRetries = maxRetries;
            this.initialRetryDelay = initialRetryDelay;
        }
    }
    
    // Add methods for all the other configuration properties
    public int getRetryLimit() {
        return 3; // Default value
    }
    
    public Duration getRetryWait() {
        return Duration.ofMillis(200); // Default value
    }
    
    public int getConcurrency() {
        return 4; // Default value
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
    
    public boolean isUseExponentialBackoff() {
        return false; // Default value
    }
    
    public Duration getMaxBackoff() {
        return Duration.ofSeconds(30); // Default value
    }
    
    public boolean isJitter() {
        return false; // Default value
    }
}