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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.pipelineframework.step.functional.OneToOne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for one-to-one pipeline steps that transform a single input item to a single output item asynchronously.
 *
 * <p>This interface represents a 1 -> 1 (async) transformation where each input item
 * is processed to produce exactly one output item. The processing is asynchronous,
 * returning a Uni that emits the transformed result.</p>
 *
 * @param <I> the type of input item
 * @param <O> the type of output item
 */
public interface StepOneToOne<I, O> extends OneToOne<I, O>, Configurable, DeadLetterQueue<I ,O> {
    /**
     * Applies the transformation to a single input item asynchronously.
     * @param in the input item to transform
     * @return a Uni that emits the transformed output item
     */
    Uni<O> applyOneToOne(I in);
    
    @Override
    default Uni<O> apply(Uni<I> input) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        Supplier<Uni<O>> uniSupplier = () -> input
            .onItem().ifNull().failWith(new NullPointerException("Input item is null"))
            .onItem().transformToUni(this::applyOneToOne);

        try {
            // Check if the input Uni is null before processing
            if (input == null) {
                if (recoverOnFailure()) {
                    return deadLetter(Uni.createFrom().failure(new NullPointerException("Input Uni is null")),
                        new NullPointerException("Input Uni is null"));
                } else {
                    return Uni.createFrom().failure(new NullPointerException("Input Uni is null"));
                }
            }

            Uni<O> uni = Uni.createFrom().deferred(() -> {
                Uni<O> result = uniSupplier.get();
                if (result == null) {
                    return Uni.createFrom().failure(new NullPointerException("Step returned null Uni"));
                }

                if (liveConfig().runWithVirtualThreads()) {
                    result = result.runSubscriptionOn(vThreadExecutor);
                }

                return result;
            });

            // Apply backpressure strategy if input is a Multi (stream)
            // For Uni, we handle it as part of the Multi pipeline in the pipeline execution
            uni = uni
                .onFailure(t -> !(t instanceof NullPointerException)).retry()
                .withBackOff(retryWait(), maxBackoff())
                .withJitter(jitter() ? 0.5 : 0.0)
                .atMost(retryLimit());

            return uni
            .onItem().invoke(i -> {
                if (debug()) {
                    LOG.debug(
                        "Step {} processed item: {}",
                        this.getClass().getSimpleName(), i
                    );
                }
            })
            .onFailure().recoverWithUni(err -> {
                if (recoverOnFailure()) {
                    if (debug()) {
                        LOG.debug(
                            "Step {0}: failed item={} after {} retries: {}",
                            this.getClass().getSimpleName(), input, retryLimit(), err
                        );
                    }
                    return deadLetter(input, err);
                } else {
                    return Uni.createFrom().failure(err);
                }
            })
            .onTermination().invoke(() -> {
                // Termination handler
            });
        } catch (Throwable t) {
            if (recoverOnFailure()) {
                if (debug()) {
                    LOG.debug(
                        "Step {0}: synchronous failure item={}: {}",
                        this.getClass().getSimpleName(), input, t
                    );
                }
                // Make sure input isn't null when calling deadLetter
                if (input == null) {
                    return deadLetter(Uni.createFrom().failure(new NullPointerException("Input Uni is null")), t);
                }
                return deadLetter(input, t);
            } else {
                return Uni.createFrom().failure(t);
            }
        }
    }
}
