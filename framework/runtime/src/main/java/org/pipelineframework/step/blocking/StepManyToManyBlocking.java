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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import org.pipelineframework.step.Configurable;
import org.pipelineframework.step.DeadLetterQueue;
import org.pipelineframework.step.functional.ManyToMany;

/**
 * Imperative variant of StepManyToMany that works with Lists instead of Multi.
 * <p>
 * This interface is designed for developers who prefer imperative programming
 * and want to work with standard Java collections instead of reactive streams.
 * <p>
 * The PipelineRunner will automatically handle the conversion between reactive
 * and imperative representations.
 */
public interface StepManyToManyBlocking<I, O> extends ManyToMany<I, O>, Configurable, DeadLetterQueue<I, O> {
    List<O> applyStreamingList(List<I> upstream);

    default List<O> deadLetterList(List<I> upstream, Throwable err) {
        System.err.print("DLQ drop");
        return Collections.emptyList();
    }

	default boolean runWithVirtualThreads() { return false; }
    
    @Override
    default Multi<O> apply(Multi<I> input) {
        final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        java.util.concurrent.Executor executor = runWithVirtualThreads() ? vThreadExecutor : null;

        // Apply overflow strategy to the input
        // default behavior - buffer with default capacity (no explicit overflow strategy needed)
        Multi<I> backpressuredInput = (executor != null) ? input.runSubscriptionOn(executor) : input;
        if ("buffer".equalsIgnoreCase(backpressureStrategy())) {
            backpressuredInput = backpressuredInput.onOverflow().buffer(backpressureBufferCapacity());
        } else if ("drop".equalsIgnoreCase(backpressureStrategy())) {
            backpressuredInput = backpressuredInput.onOverflow().drop();
        }

        return backpressuredInput
            .collect().asList()
            .onItem().transformToMulti(list -> {
                try {
                    List<O> result = applyStreamingList(list);
                    return Multi.createFrom().iterable(result);
                } catch (Exception e) {
                    if (recoverOnFailure()) {
                        List<O> dlqResult = deadLetterList(list, e);
                        return Multi.createFrom().iterable(dlqResult);
                    } else {
                        return Multi.createFrom().failure(e);
                    }
                }
            })
            .onFailure(t -> !(t instanceof NullPointerException)).retry()
            .withBackOff(retryWait(), maxBackoff())
            .withJitter(jitter() ? 0.5 : 0.0)
            .atMost(retryLimit())
            .onFailure().invoke(t -> {
                if (debug()) {
                    System.out.printf(
                        "Blocking Step %s completed all retries (%d attempts) with failure: %s%n",
                        this.getClass().getSimpleName(),
                        retryLimit(),
                        t.getMessage()
                    );
                }
            })
            .onItem().invoke(o -> {
                if (debug()) {
                    System.out.printf("Blocking Step %s streamed item: %s%n",
                            this.getClass().getSimpleName(), o);
                }
            });
    }
}