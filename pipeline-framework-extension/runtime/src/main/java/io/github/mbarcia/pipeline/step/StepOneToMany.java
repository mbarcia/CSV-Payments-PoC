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

package io.github.mbarcia.pipeline.step;

import io.github.mbarcia.pipeline.step.functional.OneToMany;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1 -> N */
public interface StepOneToMany<I, O> extends OneToMany<I, O>, Configurable, DeadLetterQueue<I, O> {
    Multi<O> applyOneToMany(I in);
    
    @Override
    default Multi<O> apply(Uni<I> input) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        Executor executor = runWithVirtualThreads() ? vThreadExecutor : null;

        return input.onItem().transformToMulti(item -> {
            Multi<O> multi = applyOneToMany(item);

            // Apply overflow strategy
            if ("buffer".equalsIgnoreCase(backpressureStrategy())) {
                multi = multi.onOverflow().buffer(backpressureBufferCapacity());
            } else if ("drop".equalsIgnoreCase(backpressureStrategy())) {
                multi = multi.onOverflow().drop();
            } else {
                // default behavior - buffer with default capacity
                multi = multi.onOverflow().buffer(128); // default buffer size
            }

            if (executor != null) {
                // shift blocking subscription work to virtual threads
                multi = multi.runSubscriptionOn(executor);
            }

            return multi.onItem().transform(o -> {
                if (debug()) {
                    LOG.debug(
                        "Step {} emitted item: {}",
                        this.getClass().getSimpleName(), o
                    );
                }
                return o;
            });
        })
        .onFailure().retry()
        .withBackOff(retryWait(), maxBackoff())
        .withJitter(jitter() ? 0.5 : 0.0)
        .atMost(retryLimit());
    }
}
