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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Objects;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.step.functional.ManyToOne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** N -> 1 (reactive) */
public interface StepManyToOne<I, O> extends Configurable, ManyToOne<I, O>, DeadLetterQueue<I, O> {
    /**
     * Apply the step to a batch of inputs, producing a single output reactively.
     *
     * @param inputs The batch inputs as a Multi
     * @return The single output as a Uni
     */
    Uni<O> applyBatchMulti(Multi<I> inputs);

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
    default long batchTimeoutMs() {
        return 1000;
    }

    /**
     * Handle a failed batch by sending it to a dead letter queue or similar mechanism reactively.
     *
     * @param inputs The batch inputs as a Multi that failed to process
     * @param error The error that occurred
     * @return The result of dead letter handling as a Uni (can be null)
     */
    default Uni<O> deadLetterBatch(Multi<I> inputs, Throwable error) {
        Logger LOG = LoggerFactory.getLogger(this.getClass());
        return inputs.collect().asList()
            .onItem().invoke(list -> LOG.error("DLQ drop for batch of {} items: {}", list.size(), error.getMessage()))
            .onItem().transformToUni(_ -> Uni.createFrom().nullItem());
    }

    @Override
    default Uni<O> applyReduce(Multi<I> input) {
        Logger LOG = LoggerFactory.getLogger(this.getClass());
        
        // Retrieve configuration
        StepConfig config = effectiveConfig();
        
        // Use configured batch size if available, otherwise use the default
        int batchSize = config != null ? config.batchSize() : this.batchSize();
        // Use configured batch timeout if available, otherwise use the default
        long batchTimeoutMs = config != null ? config.batchTimeout().toMillis() : this.batchTimeoutMs();

        // Apply overflow strategy to the input
        Multi<I> backpressuredInput = input;
        if ("buffer".equalsIgnoreCase(backpressureStrategy())) {
            backpressuredInput = backpressuredInput.onOverflow().buffer(backpressureBufferCapacity());
        } else if ("drop".equalsIgnoreCase(backpressureStrategy())) {
            backpressuredInput = backpressuredInput.onOverflow().drop();
        } else {
            // default behavior - buffer with default capacity
            backpressuredInput = backpressuredInput.onOverflow().buffer(128); // default buffer size
        }

        Multi<Multi<I>> batches = backpressuredInput.group().intoLists().of(batchSize, Duration.ofMillis(batchTimeoutMs))
            .onItem().transform(list -> Multi.createFrom().iterable(list)); // convert List<I> to Multi<I> for applyBatchMulti

        return batches
            .onItem().transformToUniAndConcatenate(batch ->
                applyBatchMulti(batch)
                    .onItem().invoke(result -> {
                        if (debug()) {
                            LOG.debug(
                                "Reactive Step {} processed batch into single output: {}",
                                this.getClass().getSimpleName(), result
                            );
                        }
                    })
                    .onFailure().recoverWithUni(error -> {
                        if (recoverOnFailure()) {
                            if (debug()) {
                                LOG.debug(
                                    "Reactive Step {}: failed batch: {}",
                                    this.getClass().getSimpleName(), error.getMessage()
                                );
                            }
                            return deadLetterBatch(batch, error);
                        } else {
                            return Uni.createFrom().failure(error);
                        }
                    })
                )
                .onFailure(t -> !(t instanceof NullPointerException)).retry()
                .withBackOff(retryWait(), maxBackoff())
                .withJitter(jitter() ? 0.5 : 0.0)
                .atMost(retryLimit())
                .filter(Objects::nonNull)
                .collect().last();
    }
}