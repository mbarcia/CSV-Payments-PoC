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

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration interface for pipeline settings.
 * This follows the pattern of using @ConfigMapping for type-safe configuration
 * with built-in defaults and easy externalization.
 * <p>
 * All pipeline-level configuration properties with their defaults.
 * These serve as defaults for individual steps unless overridden.
 */
@ConfigMapping(prefix = "csv-poc.pipeline")
public interface PipelineInitialConfig {
    /**
     * Maximum number of retry attempts for failed operations.
     * @return maximum retry attempts
     */
    @WithDefault("3")
    Integer retryLimit();

    /**
     * Base delay between retries in milliseconds.
     * @return retry delay in milliseconds
     */
    @WithDefault("200")
    Long retryWaitMs();

    /**
     * Maximum concurrent operations.
     * @return concurrency limit
     */
    @WithDefault("4")
    Integer concurrency();

    /**
     * Enable debug logging.
     * @return true to enable debug logging, false otherwise
     */
    @WithDefault("false")
    Boolean debug();

    /**
     * Enable failure recovery.
     * @return true to enable recovery, false otherwise
     */
    @WithDefault("false")
    Boolean recoverOnFailure();

    /**
     * Use virtual threads for execution.
     * @return true to use virtual threads, false otherwise
     */
    @WithDefault("false")
    Boolean runWithVirtualThreads();

    /**
     * Maximum backoff time in milliseconds.
     * @return maximum backoff time in milliseconds
     */
    @WithDefault("30000")
    Long maxBackoffMs();

    /**
     * Add jitter to retry delays.
     * @return true to add jitter, false otherwise
     */
    @WithDefault("false")
    Boolean jitter();

    /**
     * Maximum number of concurrent records to process.
     * @return concurrency limit for records processing
     */
    @WithDefault("1000")
    Integer concurrencyLimitRecords();

    /**
     * Maximum number of retry attempts for failed operations.
     * @return maximum retry attempts
     */
    @WithDefault("3")
    Integer maxRetries();

    /**
     * Initial delay in milliseconds before the first retry.
     * @return initial retry delay in milliseconds
     */
    @WithDefault("1000")
    Long initialRetryDelay();
}