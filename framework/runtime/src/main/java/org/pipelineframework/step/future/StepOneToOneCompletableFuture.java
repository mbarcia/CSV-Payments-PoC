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
import org.jboss.logging.Logger;
import org.pipelineframework.step.Configurable;
import org.pipelineframework.step.DeadLetterQueue;
import org.pipelineframework.step.functional.OneToOne;

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

    @Override
    default Uni<O> apply(Uni<I> inputUni) {
        final Logger LOG = Logger.getLogger(this.getClass());

        return inputUni
            .onItem().transformToUni(input -> {
                // call applyAsync on the plain input
                CompletableFuture<O> future = applyAsync(input);

                // wrap it into a Uni
                return Uni.createFrom().completionStage(future);
            })
            // retry / backoff / jitter
            .onFailure(t -> !(t instanceof NullPointerException)).retry()
            .withBackOff(retryWait(), maxBackoff())
            .withJitter(jitter() ? 0.5 : 0.0)
            .atMost(retryLimit())
            .onFailure().invoke(t -> {
                LOG.infof(
                    "Step %s completed all retries (%s attempts) with failure: %s",
                    this.getClass().getSimpleName(),
                    retryLimit(),
                    t.getMessage()
                );
            })
            // debug logging
            .onItem().invoke(i -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("Step %s processed item: %s", this.getClass().getSimpleName(), i);
                }
            })
            // recover with dead letter queue if needed
            .onFailure().recoverWithUni(err -> {
                if (recoverOnFailure()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debugf(
                                "Step %s: failed item=%s after %s retries: %s",
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