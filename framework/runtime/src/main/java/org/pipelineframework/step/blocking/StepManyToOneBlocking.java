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

package org.pipelineframework.step.blocking;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import org.pipelineframework.step.Configurable;
import org.pipelineframework.step.DeadLetterQueue;
import org.pipelineframework.step.functional.ManyToOne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** N -> 1 (imperative) */
public interface StepManyToOneBlocking<I, O> extends Configurable, ManyToOne<I, O>, DeadLetterQueue<I, O> {
    /**
     * Apply the step to a batch of inputs, producing a single output.
     *
     * @param inputs The list of inputs to process
     * @return The single output
     */
    O applyBatchList(List<I> inputs);

    /**
     * The batch size for collecting inputs before processing.
     *
     * @return The batch size (default: 10)
     */
    default int batchSize() {
        return 10;
    }

    /**
     * The time window in milliseconds to wait before processing a batch,
     * even if the batch size hasn't been reached.
     *
     * @return The time window in milliseconds (default: 1000ms)
     */
    default Duration batchTimeout() {
        return Duration.ofMillis(1000);
    }

    /**
     * Handle a failed batch by sending it to a dead letter queue or similar mechanism.
     *
     * @param inputs The list of inputs that failed to process
     * @param error The error that occurred
     * @return The result of dead letter handling (can be null)
     */
    default Uni<O> deadLetterBatchList(List<I> inputs, Throwable error) {
        System.err.printf("DLQ drop for batch of %d items: %s%n", inputs.size(), error.getMessage());
        return Uni.createFrom().item((O) null);
    }

    @Override
    default Uni<O> apply(Multi<I> input) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        final java.util.concurrent.Executor executor = runWithVirtualThreads() ? vThreadExecutor : null;
        int batchSize = this.batchSize();
        Duration batchTimeout = this.batchTimeout();

        // Apply overflow strategy to the input
        // default behavior - buffer with default capacity (no explicit overflow strategy needed)
        Multi<I> backpressuredInput = (executor != null)
                ? input.runSubscriptionOn(executor)
                : input;
        if ("buffer".equalsIgnoreCase(backpressureStrategy())) {
            backpressuredInput = backpressuredInput.onOverflow().buffer(backpressureBufferCapacity());
        } else if ("drop".equalsIgnoreCase(backpressureStrategy())) {
            backpressuredInput = backpressuredInput.onOverflow().drop();
        }

        Multi<List<I>> batches = backpressuredInput
            .group().intoLists().of(batchSize, batchTimeout);

        if (effectiveConfig().parallel()) {
            // Process batches concurrently
            return batches
                .onItem().transformToUniAndMerge(list -> {
                    try {
                        O result = applyBatchList(list);

                        if (debug()) {
                            LOG.debug(
                                "Blocking Step {} processed batch of {} items into single output: {}",
                                this.getClass().getSimpleName(), list.size(), result
                            );
                        }

                        return Uni.createFrom().item(result);
                    } catch (Exception e) {
                        if (recoverOnFailure()) {
                            if (debug()) {
                                LOG.debug(
                                    "Blocking Step {}: failed batch: {}",
                                    this.getClass().getSimpleName(), e.getMessage()
                                );
                            }
                            return deadLetterBatchList(list, e);
                        } else {
                            return Uni.createFrom().failure(e);
                        }
                    }
                })
                .onFailure(t -> !(t instanceof NullPointerException)).retry()
                .withBackOff(retryWait(), maxBackoff())
                .withJitter(jitter() ? 0.5 : 0.0)
                .atMost(retryLimit())
            .collect().last();
        } else {
            // Process batches sequentially (backward compatibility)
            return batches
                .onItem().transformToUniAndConcatenate(list -> {
                    try {
                        O result = applyBatchList(list);

                        if (debug()) {
                            LOG.debug(
                                "Blocking Step {} processed batch of {} items into single output: {}",
                                this.getClass().getSimpleName(), list.size(), result
                            );
                        }

                        return Uni.createFrom().item(result);
                    } catch (Exception e) {
                        if (recoverOnFailure()) {
                            if (debug()) {
                                LOG.debug(
                                    "Blocking Step {}: failed batch: {}",
                                    this.getClass().getSimpleName(), e.getMessage()
                                );
                            }
                            return deadLetterBatchList(list, e);
                        } else {
                            return Uni.createFrom().failure(e);
                        }
                    }
                })
                .onFailure(t -> !(t instanceof NullPointerException)).retry()
                .withBackOff(retryWait(), maxBackoff())
                .withJitter(jitter() ? 0.5 : 0.0)
                .atMost(retryLimit())
            .collect().last();
        }
    }
}