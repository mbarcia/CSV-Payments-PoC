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

package org.pipelineframework.step.future;

import io.smallrye.mutiny.Uni;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import org.pipelineframework.step.Configurable;
import org.pipelineframework.step.DeadLetterQueue;
import org.pipelineframework.step.functional.OneToOne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imperative variant of StepOneToOne that works with CompletableFuture instead of Uni.
 * <p>
 * This interface is designed for developers who prefer imperative programming
 * and want to work with standard Java CompletableFuture instead of Mutiny Uni.
 * <p>
 * The PipelineRunner will automatically handle the conversion between reactive
 * and imperative representations.
 */
public interface StepOneToOneCompletableFuture<I, O> extends OneToOne<I, O>, Configurable, DeadLetterQueue<I, O> {
    CompletableFuture<O> applyAsync(I in);

    default Executor getExecutor() { 
        return ForkJoinPool.commonPool(); 
    }

	default boolean runWithVirtualThreads() { return false; }

    @Override
    default Uni<O> apply(Uni<I> inputUni) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        final boolean runWithVirtualThreads = runWithVirtualThreads();
        final Executor executor = runWithVirtualThreads ? vThreadExecutor : null;

        return inputUni
            .onItem().transformToUni(input -> {
                // call applyAsync on the plain input
                CompletableFuture<O> future = applyAsync(input);

                // wrap it into a Uni
                Uni<O> uni = Uni.createFrom().completionStage(future);

                // optionally run on a specific executor
                if (executor != null) {
                    uni = uni.runSubscriptionOn(executor);
                }
                return uni;
            })
            // retry / backoff / jitter
            .onFailure(t -> !(t instanceof NullPointerException)).retry()
            .withBackOff(retryWait(), maxBackoff())
            .withJitter(jitter() ? 0.5 : 0.0)
            .atMost(retryLimit())
            .onFailure().invoke(t -> {
                if (debug()) {
                    LOG.info(
                        "Step {} completed all retries ({} attempts) with failure: {}",
                        this.getClass().getSimpleName(),
                        retryLimit(),
                        t.getMessage()
                    );
                }
            })
            // debug logging
            .onItem().invoke(i -> {
                if (debug()) {
                    LOG.debug("Step {} processed item: {}", this.getClass().getSimpleName(), i);
                }
            })
            // recover with dead letter queue if needed
            .onFailure().recoverWithUni(err -> {
                if (recoverOnFailure()) {
                    if (debug()) {
                        LOG.debug(
                                "Step {0}: failed item={} after {} retries: {}",
                                this.getClass().getSimpleName(), inputUni, retryLimit(), err
                        );
                    }
                    return deadLetter(inputUni, err);
                } else {
                    return Uni.createFrom().failure(err);
                }
            })
            .onTermination().invoke(() -> {
                // optional termination handler
            });
    }
}