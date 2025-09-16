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

package io.github.mbarcia.pipeline.step.blocking;

import io.github.mbarcia.pipeline.step.Step;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blocking variant of StepOneToOne that works with synchronous operations.
 * 
 * This interface is designed for developers who prefer blocking programming
 * and want to work with standard Java synchronous operations instead of reactive streams.
 * 
 * The PipelineRunner will automatically handle the conversion between reactive
 * and blocking representations, executing blocking operations on virtual threads
 * to prevent platform thread blocking.
 */
public interface StepOneToOneBlocking<I, O> extends Step {
    O apply(I in);

    default int concurrency() { return 1; } // max in-flight items per upstream item

    default boolean runWithVirtualThreads() { return true; }
    
    @Override
    default Multi<Object> apply(Multi<Object> input) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        return input.onItem().transformToUniAndMerge(item -> {
            Supplier<Uni<O>> uniSupplier = () -> Uni.createFrom().item(apply((I) item))
                .runSubscriptionOn(vThreadExecutor);
            
            try {
                return Uni.createFrom().deferred(() -> {
                    Uni<O> uni = uniSupplier.get();
                    if (uni == null) {
                        return Uni.createFrom().failure(new NullPointerException("Step returned null Uni"));
                    }
                    return uni;
                })
                .onFailure().retry()
                .withBackOff(retryWait(), maxBackoff())
                .withJitter(jitter() ? 0.5 : 0.0)
                .atMost(retryLimit())
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
                                this.getClass().getSimpleName(), item, retryLimit(), err
                            );
                        }
                        return deadLetter(item, err).onItem().transform(_ -> (O) item);
                    } else {
                        return Uni.createFrom().failure(err);
                    }
                })
                .onTermination().invoke(() -> {
                    // Termination handler
                })
                .onItem().transform(o -> (Object) o);
            } catch (Throwable t) {
                if (recoverOnFailure()) {
                    if (debug()) {
                        LOG.debug(
                            "Step {0}: synchronous failure item={}: {}",
                            this.getClass().getSimpleName(), item, t
                        );
                    }
                    return deadLetter(item, t).onItem().transform(_ -> (O) item)
                        .onItem().transform(o -> (Object) o);
                } else {
                    return Uni.createFrom().failure(t)
                        .onItem().transform(o -> o);
                }
            }
        });
    }
}