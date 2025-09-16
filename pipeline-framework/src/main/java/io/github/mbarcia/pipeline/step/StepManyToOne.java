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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** N -> 1 */
public interface StepManyToOne<I, O> extends Step {
    /**
     * Apply the step to a batch of inputs, producing a single output.
     * 
     * @param inputs The list of inputs to process
     * @return A Uni that completes with the single output
     */
    Uni<O> applyBatch(List<I> inputs);
    
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
     * Handle a failed batch by sending it to a dead letter queue or similar mechanism.
     * 
     * @param inputs The list of inputs that failed to process
     * @param error The error that occurred
     * @return A Uni that completes when the dead letter handling is complete
     */
    default Uni<O> deadLetterBatch(List<I> inputs, Throwable error) {
        System.err.printf("DLQ drop for batch of %d items: %s%n", inputs.size(), error.getMessage());
        return Uni.createFrom().nullItem();
    }
    
    @Override
    default Multi<Object> apply(Multi<Object> input) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        java.util.concurrent.Executor executor = runWithVirtualThreads() ? vThreadExecutor : null;
        int batchSize = this.batchSize();
        long batchTimeoutMs = this.batchTimeoutMs();

        return input
            .group().intoLists().of(batchSize, java.time.Duration.ofMillis(batchTimeoutMs))
            .onItem().transformToUniAndMerge(list -> {
                Uni<O> batchUni = applyBatch((List<I>) list)
                    .onItem().invoke(o -> {
                        if (debug()) {
                            LOG.debug(
                                "Step {} processed batch of {} items into single output: {}",
                                this.getClass().getSimpleName(), list.size(), o
                            );
                        }
                    })
                    .onFailure().recoverWithUni(err -> {
                        if (recoverOnFailure()) {
                            if (debug()) {
                                LOG.debug(
                                    "Step {}: failed batch of {} items after {} retries: {}",
                                    this.getClass().getSimpleName(), list.size(), retryLimit(), err
                                );
                            }
                            return Uni.createFrom().item((O) null);
                        } else {
                            return Uni.createFrom().failure(new RuntimeException(MessageFormat.format("Step failed: {0}", err.toString())));
                        }
                    });

                Uni<Object> uni = batchUni.onItem().transform(o -> (Object) o);
                
                if (executor != null) {
                    uni = uni.runSubscriptionOn(executor);
                }

                return uni;
            })
            .filter(Objects::nonNull); // Filter out null results from failed batches
    }
}