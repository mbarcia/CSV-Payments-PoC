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

package io.github.mbarcia.pipeline.config;

import static java.text.MessageFormat.format;

import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public final class PipelineConfig {

    private final java.util.Map<String, StepConfig> profiles = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicReference<String> activeProfile = new java.util.concurrent.atomic.AtomicReference<>();

    public PipelineConfig() {
        // initialize with a default profile
        StepConfig defaultConfig = new StepConfig();
        defaultConfig.autoPersist(true); // Enable auto-persistence by default
        profiles.put("default", defaultConfig);

        // sync with Quarkus profile
        String quarkusProfile = LaunchMode.current().getProfileKey();
        if (quarkusProfile == null || quarkusProfile.isBlank()) {
            quarkusProfile = "default";
        }
        activate(quarkusProfile);
    }

    /** Which profile is currently active */
    public String activeProfile() {
        return activeProfile.get();
    }

    /** Switch active profile (must already exist or will be created) */
    public void activate(String profileName) {
        profiles.computeIfAbsent(profileName, _ -> new StepConfig());
        activeProfile.set(profileName);
    }

    /** Get the StepConfig for the current profile */
    public StepConfig defaults() {
        return profiles.get(activeProfile());
    }

    /** Add or update a profile explicitly */
    public PipelineConfig profile(String name, StepConfig config) {
        profiles.put(name, config);
        return this;
    }

    /** Create a new StepConfig initialized with active profile defaults */
    public StepConfig newStepConfig() {
        StepConfig base = defaults();
        return new StepConfig()
                .autoPersist(base.autoPersist())
                .retryLimit(base.retryLimit())
                .retryWait(base.retryWait())
                .concurrency(base.concurrency())
                .debug(base.debug())
                .recoverOnFailure(base.recoverOnFailure())
                .runWithVirtualThreads(base.runWithVirtualThreads())
                .maxBackoff(base.maxBackoff())
                .jitter(base.jitter());
    }

    @Override
    public String toString() {
        return format("PipelineConfig'{'active={0}, profiles={1}'}'", activeProfile(), profiles.keySet());
    }
}