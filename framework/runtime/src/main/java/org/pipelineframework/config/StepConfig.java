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

import jakarta.enterprise.context.Dependent;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configuration class for pipeline steps that holds various runtime parameters
 * such as retry limits, concurrency settings, and debugging options.
 */
@Dependent
public class StepConfig {

    private final AtomicInteger retryLimit = new AtomicInteger(3);
    private final AtomicReference<Duration> retryWait = new AtomicReference<>(Duration.ofMillis(200));
    private final AtomicInteger concurrency = new AtomicInteger(4);
    private final AtomicInteger backpressureBufferCapacity = new AtomicInteger(1024);
    private final AtomicInteger batchSize = new AtomicInteger(10); // Default batch size
    private final AtomicReference<Duration> batchTimeout = new AtomicReference<>(Duration.ofMillis(1000)); // Default batch timeout

    private volatile boolean debug = false;
    private volatile boolean recoverOnFailure = false;
    private volatile boolean runWithVirtualThreads = false;
    private volatile boolean autoPersist = false;
    private volatile String backpressureStrategy = "BUFFER";

    private final AtomicReference<Duration> maxBackoff = new AtomicReference<>(Duration.ofSeconds(30));
    private volatile boolean jitter = false;

    /**
     * Creates a new StepConfig with default values.
     */
    public StepConfig() {}

    // --- getters ---
    /**
     * Whether to automatically persist step results to storage
     * @return true if auto persistence is enabled, false otherwise
     */
    public boolean autoPersist() { return autoPersist; } // New getter
    /**
     * Maximum number of retries for failed operations
     * @return the retry limit
     */
    public int retryLimit() { return retryLimit.get(); }
    /**
     * Duration to wait between retries
     * @return the retry wait duration
     */
    public Duration retryWait() { return retryWait.get(); }
    /**
     * Maximum number of concurrent operations allowed
     * @return the concurrency limit
     */
    public int concurrency() { return concurrency.get(); }
    /**
     * Buffer capacity for handling backpressure
     * @return the backpressure buffer capacity
     */
    public int backpressureBufferCapacity() { return backpressureBufferCapacity.get(); }
    /**
     * Number of items to batch together before processing
     * @return the batch size
     */
    public int batchSize() { return batchSize.get(); }
    /**
     * Maximum time to wait before processing a batch, even if batch size hasn't been reached
     * @return the batch timeout duration
     */
    public Duration batchTimeout() { return batchTimeout.get(); }
    /**
     * Strategy to use for handling backpressure ("BUFFER", "DROP", etc.)
     * @return the backpressure strategy
     */
    public String backpressureStrategy() { return backpressureStrategy; }
    /**
     * Whether to enable debug mode with additional logging
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean debug() { return debug; }
    /**
     * Whether to attempt recovery when a failure occurs
     * @return true if failure recovery is enabled, false otherwise
     */
    public boolean recoverOnFailure() { return recoverOnFailure; }
    /**
     * Whether to run the step using virtual threads
     * @return true if virtual threads are enabled, false otherwise
     */
    public boolean runWithVirtualThreads() { return runWithVirtualThreads; }
    /**
     * Maximum backoff duration when using exponential backoff
     * @return the maximum backoff duration
     */
    public Duration maxBackoff() { return maxBackoff.get(); }
    /**
     * Whether to add jitter to backoff intervals
     * @return true if jitter is enabled, false otherwise
     */
    public boolean jitter() { return jitter; }

    // --- setters (runtime mutable) ---
    /**
     * Sets whether to automatically persist step results to storage
     * @param v true to enable auto persistence, false to disable
     * @return this StepConfig instance for method chaining
     */
    public StepConfig autoPersist(boolean v) { autoPersist = v; return this; } // New setter
    /**
     * Sets the maximum number of retries for failed operations
     * @param v the retry limit
     * @return this StepConfig instance for method chaining
     */
    public StepConfig retryLimit(int v) { retryLimit.set(v); return this; }
    /**
     * Sets the duration to wait between retries
     * @param v the retry wait duration
     * @return this StepConfig instance for method chaining
     */
    public StepConfig retryWait(Duration v) { retryWait.set(Objects.requireNonNull(v)); return this; }
    /**
     * Sets the maximum number of concurrent operations allowed
     * @param v the concurrency limit
     * @return this StepConfig instance for method chaining
     */
    public StepConfig concurrency(int v) { concurrency.set(v); return this; }
    /**
     * Sets the buffer capacity for handling backpressure
     * @param v the backpressure buffer capacity
     * @return this StepConfig instance for method chaining
     */
    public StepConfig backpressureBufferCapacity(int v) { backpressureBufferCapacity.set(v); return this; }
    /**
     * Sets the number of items to batch together before processing
     * @param v the batch size
     * @return this StepConfig instance for method chaining
     */
    public StepConfig batchSize(int v) { batchSize.set(v); return this; }
    /**
     * Sets the maximum time to wait before processing a batch, even if batch size hasn't been reached
     * @param v the batch timeout duration
     * @return this StepConfig instance for method chaining
     */
    public StepConfig batchTimeout(Duration v) { batchTimeout.set(Objects.requireNonNull(v)); return this; }
    /**
     * Sets the strategy to use for handling backpressure
     * @param v the backpressure strategy ("BUFFER", "DROP", etc.)
     * @return this StepConfig instance for method chaining
     */
    public StepConfig backpressureStrategy(String v) { backpressureStrategy = v; return this; }
    /**
     * Sets whether to enable debug mode with additional logging
     * @param v true to enable debug mode, false to disable
     * @return this StepConfig instance for method chaining
     */
    public StepConfig debug(boolean v) { debug = v; return this; }
    /**
     * Sets whether to attempt recovery when a failure occurs
     * @param v true to enable failure recovery, false to disable
     * @return this StepConfig instance for method chaining
     */
    public StepConfig recoverOnFailure(boolean v) { recoverOnFailure = v; return this; }
    /**
     * Sets whether to run the step using virtual threads
     * @param v true to enable virtual threads, false to disable
     * @return this StepConfig instance for method chaining
     */
    public StepConfig runWithVirtualThreads(boolean v) { runWithVirtualThreads = v; return this; }
    /**
     * Sets the maximum backoff duration when using exponential backoff
     * @param v the maximum backoff duration
     * @return this StepConfig instance for method chaining
     */
    public StepConfig maxBackoff(Duration v) { maxBackoff.set(Objects.requireNonNull(v)); return this; }
    /**
     * Sets whether to add jitter to backoff intervals
     * @param v true to enable jitter, false to disable
     * @return this StepConfig instance for method chaining
     */
    public StepConfig jitter(boolean v) { jitter = v; return this; }

    @Override
    public String toString() {
        return MessageFormat.format("StepConfig'{'retryLimit={0}, retryWait={1}, concurrency={2}, debug={3}, recoverOnFailure={4}, runWithVirtualThreads={5}, maxBackoff={6}, jitter={7}, autoPersist={8}'}'",
                retryLimit(),
                retryWait(),
                concurrency(),
                debug,
                recoverOnFailure,
                runWithVirtualThreads,
                maxBackoff(),
                jitter,
                autoPersist);
    }
}