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

package io.github.mbarcia.pipeline.step;

import io.github.mbarcia.pipeline.config.StepConfig;
import io.smallrye.mutiny.Multi;

public interface Step {
    StepConfig effectiveConfig();

    default int retryLimit() { return effectiveConfig().retryLimit(); }
    default java.time.Duration retryWait() { return effectiveConfig().retryWait(); }
    default boolean debug() { return effectiveConfig().debug(); }
    default boolean recoverOnFailure() { return effectiveConfig().recoverOnFailure(); }
    default boolean runWithVirtualThreads() { return effectiveConfig().runWithVirtualThreads(); }
    default java.time.Duration maxBackoff() { return effectiveConfig().maxBackoff(); }
    default boolean jitter() { return effectiveConfig().jitter(); }

    default io.smallrye.mutiny.Uni<Void> deadLetter(Object failedItem, Throwable cause) {
        System.err.printf("DLQ drop: item=%s cause=%s%n", failedItem, cause);
        return io.smallrye.mutiny.Uni.createFrom().voidItem();
    }

    /**
     * Apply this step to the input stream and return the output stream.
     * 
     * @param input The input stream to process
     * @return The output stream after applying this step
     */
    Multi<Object> apply(Multi<Object> input);
}
