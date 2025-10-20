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

import java.time.Duration;
import java.util.Objects;

/**
 * Live configuration for pipeline steps that can be modified at runtime.
 * <p>
 * This class extends {@link StepConfig} to provide mutable configuration properties
 * that can be changed during application runtime. It uses atomic references and
 * volatile fields to ensure thread-safe updates to configuration values.
 */
public final class LiveStepConfig extends StepConfig {

    private final PipelineConfig pipelineConfig;
    private final StepConfig overrides;

    /**
     * Creates a new LiveStepConfig with the provided overrides and pipeline configuration.
     *
     * @param overrides the configuration overrides
     * @param pipelineConfig the pipeline configuration
     */
    public LiveStepConfig(StepConfig overrides, PipelineConfig pipelineConfig) {
        this.overrides = Objects.requireNonNull(overrides, "overrides must not be null");
        this.pipelineConfig = Objects.requireNonNull(pipelineConfig, "pipelineConfig must not be null");
    }

    // --- getters ---
    /**
     * Get the configuration overrides
     * @return the configuration overrides
     */
    public StepConfig overrides() {
        return overrides;
    }

    /**
     * Get the current pipeline configuration defaults
     * @return the current pipeline configuration defaults
     */
    private StepConfig currentDefaults() {
        return pipelineConfig.defaults();
    }

    // all getters delegate: override if set, else defaults
    @Override public int retryLimit() {
        return overrides().retryLimit() != super.retryLimit() ? overrides().retryLimit() : currentDefaults().retryLimit();
    }
    @Override public Duration retryWait() {
        Duration o = overrides().retryWait();
        return !o.equals(super.retryWait()) ? o : currentDefaults().retryWait();
    }
    @Override public boolean debug() {
        return overrides().debug() != super.debug() ? overrides().debug() : currentDefaults().debug();
    }
    @Override public boolean recoverOnFailure() {
        return overrides().recoverOnFailure() != super.recoverOnFailure() ? overrides().recoverOnFailure() : currentDefaults().recoverOnFailure();
    }
    @Override public boolean runWithVirtualThreads() {
        return overrides().runWithVirtualThreads() != super.runWithVirtualThreads() ? overrides().runWithVirtualThreads() : currentDefaults().runWithVirtualThreads();
    }
    @Override public Duration maxBackoff() {
        Duration o = overrides().maxBackoff();
        return !o.equals(super.maxBackoff()) ? o : currentDefaults().maxBackoff();
    }
    @Override public boolean jitter() {
        return overrides().jitter() != super.jitter() ? overrides().jitter() : currentDefaults().jitter();
    }
    @Override public boolean autoPersist() {
        return overrides().autoPersist() != super.autoPersist() ? overrides().autoPersist() : currentDefaults().autoPersist();
    }
    @Override public int backpressureBufferCapacity() {
        return overrides().backpressureBufferCapacity() != super.backpressureBufferCapacity() ? 
               overrides().backpressureBufferCapacity() : currentDefaults().backpressureBufferCapacity();
    }
    @Override public String backpressureStrategy() {
        return !overrides().backpressureStrategy().equals(super.backpressureStrategy()) ? 
               overrides().backpressureStrategy() : currentDefaults().backpressureStrategy();
    }
    @Override public int batchSize() {
        return overrides().batchSize() != super.batchSize() ?
                overrides().batchSize() : currentDefaults().batchSize();
    }
    @Override public Duration batchTimeout() {
        Duration o = overrides().batchTimeout();
        return !o.equals(super.batchTimeout()) ? o : currentDefaults().batchTimeout();
    }
    @Override public boolean parallel() {
        return overrides().parallel() != super.parallel() ? 
               overrides().parallel() : currentDefaults().parallel();
    }

}