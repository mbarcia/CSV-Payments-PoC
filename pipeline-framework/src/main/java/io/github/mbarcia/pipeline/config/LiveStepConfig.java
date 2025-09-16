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

import jakarta.enterprise.context.Dependent;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Dependent
public final class LiveStepConfig extends StepConfig {

    private final PipelineConfig pipelineConfig;
    private final AtomicReference<StepConfig> overrides = new AtomicReference<>(new StepConfig());

    public LiveStepConfig(PipelineConfig pipelineConfig) {
        this.pipelineConfig = pipelineConfig;
    }

    /** Apply per-step overrides */
    public StepConfig overrides() {
        return overrides.get();
    }

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
    @Override public int concurrency() {
        return overrides().concurrency() != super.concurrency() ? overrides().concurrency() : currentDefaults().concurrency();
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

}