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
import org.pipelineframework.step.functional.ManyToOne;
import org.jboss.logging.Logger;

/** N -> 1 (reactive) */
public interface StepManyToOne<I, O> extends Configurable, ManyToOne<I, O>, DeadLetterQueue<I, O> {

    /**
     * Apply the step to a stream of inputs, producing a single output reactively.
     * This method serves as a client-side wrapper that provides cross-cutting concerns
     * like retry logic, error handling, and backpressure management for gRPC calls.
     *
     * @param input The stream of inputs as a Multi
     * @return The single output as a Uni
     */
    @Override
    default Uni<O> apply(Multi<I> input) {
        Logger LOG = Logger.getLogger(this.getClass());
        
        // Apply overflow strategy to the input if needed
        Multi<I> backpressuredInput = input;
        if ("buffer".equalsIgnoreCase(backpressureStrategy())) {
            backpressuredInput = backpressuredInput.onOverflow().buffer(backpressureBufferCapacity());
        } else if ("drop".equalsIgnoreCase(backpressureStrategy())) {
            backpressuredInput = backpressuredInput.onOverflow().drop();
        } else {
            // default behavior - buffer with default capacity
            backpressuredInput = backpressuredInput.onOverflow().buffer(128); // default buffer size
        }

        final Multi<I> finalInput = backpressuredInput;

        return applyReduce(finalInput)
            .onItem().invoke(resultValue -> {
                if (debug()) {
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
                    if (debug()) {
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
        Logger LOG = Logger.getLogger(this.getClass());
        return input.collect().asList()
            .onItem().invoke(list -> LOG.errorf("DLQ drop for stream of %s items: %s", list.size(), error.getMessage()))
            .onItem().transformToUni(_ -> Uni.createFrom().nullItem());
    }
}