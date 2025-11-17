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

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configuration class for pipeline steps that holds various runtime parameters
 * such as retry limits, concurrency settings, and debugging options.
 *
 * This class can be initialized with values from the new Quarkus configuration system.
 */
@Dependent
@DefaultBean
public class StepConfig {

    // Default values for configuration
    private static final int DEFAULT_RETRY_LIMIT = 3;
    private static final Duration DEFAULT_RETRY_WAIT = Duration.ofMillis(2000);
    private static final int DEFAULT_BACKPRESSURE_BUFFER_CAPACITY = 1024;
    private static final Duration DEFAULT_MAX_BACKOFF = Duration.ofSeconds(30);
    private static final String DEFAULT_BACKPRESSURE_STRATEGY = "BUFFER";

    // Mutable fields for runtime configuration (to maintain backward compatibility)
    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    private final AtomicReference<Duration> retryWait = new AtomicReference<>(DEFAULT_RETRY_WAIT);
    private volatile boolean parallel = false; // Default is sequential processing
    private final AtomicInteger backpressureBufferCapacity = new AtomicInteger(DEFAULT_BACKPRESSURE_BUFFER_CAPACITY);

    private volatile boolean recoverOnFailure = false;
    private volatile String backpressureStrategy = DEFAULT_BACKPRESSURE_STRATEGY;

    private final AtomicReference<Duration> maxBackoff = new AtomicReference<>(DEFAULT_MAX_BACKOFF);
    private volatile boolean jitter = false;

    /**
     * Creates a new StepConfig with default values.
     */
    public StepConfig() {}

    /**
     * Creates a StepConfig initialized with values from the Quarkus configuration.
     * @param config The configuration to use for initialization
     */
    public StepConfig(org.pipelineframework.config.PipelineStepConfig.StepConfig config) {
        if (config != null) {
            this.retryLimit.set(config.retryLimit());
            this.retryWait.set(Duration.ofMillis(config.retryWaitMs()));
            this.parallel = config.parallel();
            this.recoverOnFailure = config.recoverOnFailure();
            this.maxBackoff.set(Duration.ofMillis(config.maxBackoff()));
            this.jitter = config.jitter();
            this.backpressureBufferCapacity.set(config.backpressureBufferCapacity());
            this.backpressureStrategy = config.backpressureStrategy();
        }
    }

    // --- getters ---
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
     * Whether to attempt recovery when a failure occurs
     * @return true if failure recovery is enabled, false otherwise
     */
    public boolean recoverOnFailure() { return recoverOnFailure; }

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
     * Sets the backpressure buffer capacity
     * @param v the buffer capacity
     * @return this StepConfig instance for method chaining
     */
    public StepConfig backpressureBufferCapacity(int v) {
        if (v <= 0) {
            throw new IllegalArgumentException("backpressureBufferCapacity must be > 0");
        }
        backpressureBufferCapacity.set(v);
        return this;
    }

    /**
     * Sets whether to attempt recovery when a failure occurs
     * @param v true to enable failure recovery, false to disable
     * @return this StepConfig instance for method chaining
     */
    public StepConfig recoverOnFailure(boolean v) { recoverOnFailure = v; return this; }

    /**
     * Sets the backpressure strategy to use when buffering items ("BUFFER", "DROP")
     * @param v the backpressure strategy
     * @return this StepConfig instance for method chaining
     */
    public StepConfig backpressureStrategy(String v) {
        Objects.requireNonNull(v, "backpressureStrategy must not be null");
        String norm = v.trim().toUpperCase();
        if (!norm.equals("BUFFER") && !norm.equals("DROP")) {
            throw new IllegalArgumentException("backpressureStrategy must be BUFFER or DROP");
        }
        backpressureStrategy = norm;
        return this;
    }

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
        return MessageFormat.format("StepConfig'{'retryLimit={0}, retryWait={1}, parallel={2}, recoverOnFailure={3}, maxBackoff={4}, jitter={5}, backpressureBufferCapacity={6}, backpressureStrategy={7}'}'",
                retryLimit(),
                retryWait(),
                parallel,
                recoverOnFailure,
                maxBackoff(),
                jitter,
                backpressureBufferCapacity(),
                backpressureStrategy());
    }
}