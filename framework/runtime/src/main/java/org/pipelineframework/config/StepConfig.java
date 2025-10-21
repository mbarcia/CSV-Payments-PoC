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
    private final AtomicReference<Duration> retryWait = new AtomicReference<>(Duration.ofMillis(2000));
    private volatile boolean parallel = false; // Default is sequential processing
    private final AtomicInteger backpressureBufferCapacity = new AtomicInteger(1024);
    private final AtomicInteger batchSize = new AtomicInteger(10); // Default batch size
    private final AtomicReference<Duration> batchTimeout = new AtomicReference<>(Duration.ofMillis(1000)); // Default batch timeout

    private volatile boolean debug = false;
    private volatile boolean recoverOnFailure = false;
    private volatile boolean runWithVirtualThreads = false;
    private volatile boolean autoPersist = true;
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
    public boolean autoPersist() { return autoPersist; }
    /**
     * Number of times to retry a failed operation before giving up
     * @return the retry limit (default: 3)
     */
    public int retryLimit() { return retryLimit.get(); }
    
    /**
     * Base delay between retry attempts
     * @return the retry wait duration (default: 2000ms)
     */
    public Duration retryWait() { return retryWait.get(); }

    /**
     * The backpressure buffer capacity
     * @return the buffer capacity (default: 1024)
     */
    public int backpressureBufferCapacity() { return backpressureBufferCapacity.get(); }

    /**
     * The batch size for collecting inputs before processing
     * @return the batch size (default: 10)
     */
    public int batchSize() { return batchSize.get(); }
    
    /**
     * The time window to wait before processing a batch
     * @return the batch timeout duration (default: 1000ms)
     */
    public Duration batchTimeout() { return batchTimeout.get(); }

    /**
     * Backpressure strategy to use when buffering items ("BUFFER", "DROP")
     * @return the backpressure strategy (default: "BUFFER")
     */
    public String backpressureStrategy() { return backpressureStrategy; }

    /**
     * Whether to enable parallel processing for this step
     * @return true if parallel processing is enabled, false for sequential processing
     */
    public boolean parallel() { return parallel; }

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
     * @return the maximum backoff duration (default: 30 seconds)
     */
    public Duration maxBackoff() { return maxBackoff.get(); }
    /**
     * Whether to add jitter to backoff intervals
     * @return true if jitter is enabled, false otherwise
     */
    public boolean jitter() { return jitter; }

    // --- setters ---
    /**
     * Sets the number of times to retry a failed operation before giving up
     * @param v the retry limit
     * @return this StepConfig instance for method chaining
     */
    public StepConfig retryLimit(int v) { 
        if (v < 0) {
            throw new IllegalArgumentException("retryLimit must be >= 0");
        }
        retryLimit.set(v); 
        return this; 
    }
    /**
     * Sets the base delay between retry attempts
     * @param v the retry wait duration
     * @return this StepConfig instance for method chaining
     */
    public StepConfig retryWait(Duration v) { 
        Objects.requireNonNull(v, "retryWait must not be null");
        if (v.isNegative() || v.isZero()) {
            throw new IllegalArgumentException("retryWait must be > 0");
        }
        retryWait.set(v); 
        return this; 
    }
    /**
     * Sets whether to enable parallel processing for this step
     * @param v true to enable parallel processing, false for sequential processing
     * @return this StepConfig instance for method chaining
     */
    public StepConfig parallel(boolean v) { parallel = v; return this; }
    /**
     * Sets the batch size for collecting inputs before processing
     * @param v the batch size
     * @return this StepConfig instance for method chaining
     */
    public StepConfig batchSize(int v) { 
        if (v <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }
        batchSize.set(v); 
        return this; 
    }
    /**
     * Sets the time window to wait before processing a batch
     * @param v the batch timeout duration
     * @return this StepConfig instance for method chaining
     */
    public StepConfig batchTimeout(Duration v) { 
        Objects.requireNonNull(v, "batchTimeout must not be null");
        if (v.isNegative() || v.isZero()) {
            throw new IllegalArgumentException("batchTimeout must be > 0");
        }
        batchTimeout.set(v); 
        return this; 
    }
    /**
     * Sets the backpressure buffer capacity
     * @param v the buffer capacity
     * @return this StepConfig instance for method chaining
     */
    public StepConfig backpressureBufferCapacity(int v) { 
        if (v < 0) {
            throw new IllegalArgumentException("backpressureBufferCapacity must be >= 0");
        }
        backpressureBufferCapacity.set(v); 
        return this; 
    }
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
     * Sets whether to automatically persist step results to storage
     * @param v true to enable auto persistence, false to disable
     * @return this StepConfig instance for method chaining
     */
    public StepConfig autoPersist(boolean v) { autoPersist = v; return this; }
    /**
     * Sets the backpressure strategy to use when buffering items ("BUFFER", "DROP")
     * @param v the backpressure strategy
     * @return this StepConfig instance for method chaining
     */
    public StepConfig backpressureStrategy(String v) { backpressureStrategy = v; return this; }
    /**
     * Sets the maximum backoff duration when using exponential backoff
     * @param v the maximum backoff duration
     * @return this StepConfig instance for method chaining
     */
    public StepConfig maxBackoff(Duration v) { 
        Objects.requireNonNull(v, "maxBackoff must not be null");
        if (v.isNegative() || v.isZero()) {
            throw new IllegalArgumentException("maxBackoff must be > 0");
        }
        maxBackoff.set(v); 
        return this; 
    }
    /**
     * Sets whether to add jitter to backoff intervals
     * @param v true to enable jitter, false to disable
     * @return this StepConfig instance for method chaining
     */
    public StepConfig jitter(boolean v) { jitter = v; return this; }

    @Override
    public String toString() {
        return MessageFormat.format("StepConfig'{'retryLimit={0}, retryWait={1}, parallel={2}, debug={3}, recoverOnFailure={4}, maxBackoff={5}, jitter={6}, autoPersist={7}, batchSize={8}, batchTimeout={9}, backpressureBufferCapacity={10}'}'",
                retryLimit(),
                retryWait(),
                parallel,
                debug,
                recoverOnFailure,
                maxBackoff(),
                jitter,
                autoPersist,
                batchSize(),
                batchTimeout(),
                backpressureBufferCapacity());
    }
}