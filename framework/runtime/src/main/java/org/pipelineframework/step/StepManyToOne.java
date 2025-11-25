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
import org.jboss.logging.Logger;
import org.pipelineframework.step.functional.ManyToOne;

/**
 * N -> 1 (reactive)
 *
 * @param <I> the input type
 * @param <O> the output type
 */
public interface StepManyToOne<I, O> extends Configurable, ManyToOne<I, O>, DeadLetterQueue<I, O> {

    /** Logger for StepManyToOne operations. */
    Logger LOG = Logger.getLogger(StepManyToOne.class);

    /**
     * Apply the step to a stream of inputs and produce a single aggregated output.
     *
     * <p>The method applies the configured backpressure strategy to the provided input stream,
     * applies retry semantics on failures (excluding NullPointerException), and — if configured —
     * recovers failed processing by delegating the collected stream to the dead-letter handler.
     *
     * @param input the stream of inputs to be processed
     * @return a Uni that emits the step's single output; if retries are exhausted the Uni will
     *         either fail with the original error or, if recovery is enabled, emit the value
     *         produced by the dead-letter handling (which may be null)
     */
    @Override
    default Uni<O> apply(Multi<I> input) {
        
        // Apply overflow strategy to the input if needed
        Multi<I> backpressuredInput = input;
        final String strategy = backpressureStrategy();
        if ("buffer".equalsIgnoreCase(strategy)) {
            backpressuredInput = backpressuredInput.onOverflow().buffer(backpressureBufferCapacity());
        } else if ("drop".equalsIgnoreCase(strategy)) {
            backpressuredInput = backpressuredInput.onOverflow().drop();
        } else if (strategy == null || strategy.isBlank() || "default".equalsIgnoreCase(strategy)) {
            // default behavior - buffer with default capacity
            backpressuredInput = backpressuredInput.onOverflow().buffer(128); // default buffer size
        } else {
            LOG.warnf("Unknown backpressure strategy '%s', defaulting to buffer(128)", strategy);
            backpressuredInput = backpressuredInput.onOverflow().buffer(128); // default buffer size
        }

        final Multi<I> finalInput = backpressuredInput;

        return applyReduce(finalInput)
            .onItem().invoke(resultValue -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("Reactive Step %s processed stream into output: %s",
                        this.getClass().getSimpleName(), resultValue);
                }
            })
            .onFailure(t -> !(t instanceof NullPointerException))
            .retry()
            .withBackOff(retryWait(), maxBackoff())
            .withJitter(jitter() ? 0.5 : 0.0)
            .atMost(retryLimit())
            .onFailure().recoverWithUni(error -> {
                if (recoverOnFailure()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debugf("Reactive Step %s: failed to process stream: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    }

                    return deadLetterStream(finalInput, error);
                } else {
                    return Uni.createFrom().failure(error);
                }
            });
    }

    /**
     * Apply the reduction operation to a stream of inputs, producing a single output.
     * This method would typically be implemented by gRPC client adapters.
     *
     * @param input The stream of inputs as a Multi
     * @return The single output as a Uni
     */
    Uni<O> applyReduce(Multi<I> input);

    /**
     * Handle a failed stream by sending it to a dead letter queue or similar mechanism reactively.
     *
     * @param input The input stream that failed to process
     * @param error The error that occurred
     * @return The result of dead letter handling as a Uni (can be null)
     */
    default Uni<O> deadLetterStream(Multi<I> input, Throwable error) {
        // Use broadcast to allow multiple consumers of the same stream
        Multi<I> broadcastInput = input.broadcast().toAllSubscribers();

        // Count the number of elements and collect a small sample
        final int maxSampleSize = 5; // Only keep a few sample items to avoid memory issues

        return broadcastInput
            .select().first(maxSampleSize)
            .collect().asList()
            .onItem().transformToUni(sampleList -> {
                // Count the total number of items in the stream using a counter approach
                java.util.concurrent.atomic.AtomicLong counter = new java.util.concurrent.atomic.AtomicLong(0);

                // Create a Uni that completes when the counting stream completes
                Uni<Void> countingCompletion = Uni.createFrom().emitter(emitter -> broadcastInput
                    .invoke(ignored -> counter.incrementAndGet()) // Count items as they pass through
                    .subscribe()
                    .with(
                        ignored -> {}, // Do nothing with each item
                        failure -> {
                            // On failure, log and complete the emitter
                            String sampleInfo = !sampleList.isEmpty() ?
                                String.format("first %d items before failure",
                                              Math.min(sampleList.size(), maxSampleSize)) :
                                "stream with failure";
                            LOG.errorf("DLQ drop for %s: %s",
                                sampleInfo, failure.getMessage());
                            emitter.fail(failure);
                        },
                        () -> {
                            // On completion, log the count and complete the emitter
                            long count = counter.get();
                            String sampleInfo = !sampleList.isEmpty() ?
                                String.format("first %d of %d items",
                                              Math.min(sampleList.size(), maxSampleSize),
                                              count) :
                                String.format("%d items", count);
                            LOG.errorf("DLQ drop for stream with %s: %s",
                                sampleInfo, error.getMessage());
                            emitter.complete(null);
                        }
                    ));

                return countingCompletion
                    .onItem().transformToUni(ignored -> Uni.createFrom().nullItem());
            });
    }
}