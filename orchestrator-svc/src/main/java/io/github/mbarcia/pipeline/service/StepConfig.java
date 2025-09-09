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

import jakarta.enterprise.context.Dependent;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Dependent
public class StepConfig {

    private final AtomicInteger retryLimit = new AtomicInteger(3);
    private final AtomicReference<Duration> retryWait = new AtomicReference<>(Duration.ofMillis(200));
    private final AtomicInteger concurrency = new AtomicInteger(4);

    private volatile boolean debug = false;
    private volatile boolean recoverOnFailure = false;
    private volatile boolean runWithVirtualThreads = false;

    private volatile boolean useExponentialBackoff = false;
    private final AtomicReference<Duration> maxBackoff = new AtomicReference<>(Duration.ofSeconds(30));
    private volatile boolean jitter = false;

    public StepConfig() {}

    // --- getters ---
    public int retryLimit() { return retryLimit.get(); }
    public Duration retryWait() { return retryWait.get(); }
    public int concurrency() { return concurrency.get(); }
    public boolean debug() { return debug; }
    public boolean recoverOnFailure() { return recoverOnFailure; }
    public boolean runWithVirtualThreads() { return runWithVirtualThreads; }
    public boolean useExponentialBackoff() { return useExponentialBackoff; }
    public Duration maxBackoff() { return maxBackoff.get(); }
    public boolean jitter() { return jitter; }

    // --- setters (runtime mutable) ---
    public StepConfig retryLimit(int v) { retryLimit.set(v); return this; }
    public StepConfig retryWait(Duration v) { retryWait.set(Objects.requireNonNull(v)); return this; }
    public StepConfig concurrency(int v) { concurrency.set(v); return this; }
    public StepConfig debug(boolean v) { debug = v; return this; }
    public StepConfig recoverOnFailure(boolean v) { recoverOnFailure = v; return this; }
    public StepConfig runWithVirtualThreads(boolean v) { runWithVirtualThreads = v; return this; }
    public StepConfig useExponentialBackoff(boolean v) { useExponentialBackoff = v; return this; }
    public StepConfig maxBackoff(Duration v) { maxBackoff.set(Objects.requireNonNull(v)); return this; }
    public StepConfig jitter(boolean v) { jitter = v; return this; }

    @Override
    public String toString() {
        return MessageFormat.format("StepConfig'{'retryLimit={0}, retryWait={1}, concurrency={2}, debug={3}, recoverOnFailure={4}, runWithVirtualThreads={5}, useExponentialBackoff={6}, maxBackoff={7}, jitter={8}'}'", retryLimit(), retryWait(), concurrency(), debug, recoverOnFailure, runWithVirtualThreads, useExponentialBackoff, maxBackoff(), jitter);
    }
}