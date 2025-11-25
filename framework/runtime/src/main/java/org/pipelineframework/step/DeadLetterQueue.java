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

package org.pipelineframework.step;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

/**
 * Interface for pipeline steps that support dead letter queue functionality.
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public interface DeadLetterQueue<I, O> {

    /** Logger for dead letter queue operations. */
    Logger LOG = Logger.getLogger(DeadLetterQueue.class);

    /**
     * Record a failed item to the dead-letter queue and produce a null output.
     *
     * Logs an error-level message containing the failed item (via its `toString`) and the failure cause,
     * then returns a Uni that completes with a `null` item of type `O`.
     *
     * @param failedItem the failed input wrapped in a Uni; its `toString` is included in the log
     * @param cause the throwable that caused the failure
     * @return a Uni that emits a `null` value of type `O`
     */
    default Uni<O> deadLetter(Uni<I> failedItem, Throwable cause) {
        LOG.errorf("DLQ drop: item=%s cause=%s", failedItem.toString(), cause);
        return io.smallrye.mutiny.Uni.createFrom().nullItem();
    }
}