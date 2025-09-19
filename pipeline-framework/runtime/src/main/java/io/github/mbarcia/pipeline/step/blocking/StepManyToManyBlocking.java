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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Imperative variant of StepManyToMany that works with Lists instead of Multi.
 * 
 * This interface is designed for developers who prefer imperative programming
 * and want to work with standard Java collections instead of reactive streams.
 * 
 * The PipelineRunner will automatically handle the conversion between reactive
 * and imperative representations.
 */
public interface StepManyToManyBlocking extends Step {
    List<Object> applyStreamingList(List<Object> upstream);

    default List<Object> deadLetterList(List<Object> upstream, Throwable err) {
        System.err.print("DLQ drop");
        return Collections.emptyList();
    }

    default boolean runWithVirtualThreads() { return false; }
    
    @Override
    default Multi<Object> apply(Multi<Object> input) {
        final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        java.util.concurrent.Executor executor = runWithVirtualThreads() ? vThreadExecutor : null;

        return Multi.createFrom().deferred(() -> {
            Multi<Object> baseUpstream = (executor != null)
                    ? input.runSubscriptionOn(executor)
                    : input;

            // Collect items into a list, process blocking, then convert back to Multi
            return baseUpstream
                .collect().asList()
                .onItem().transformToMulti(list -> {
                    try {
                        List<Object> result = applyStreamingList(list);
                        return Multi.createFrom().iterable(result);
                    } catch (Exception e) {
                        if (recoverOnFailure()) {
                            List<Object> dlqResult = deadLetterList(list, e);
                            return Multi.createFrom().iterable(dlqResult);
                        } else {
                            return Multi.createFrom().failure(e);
                        }
                    }
                })
                .onFailure().retry()
                .withBackOff(retryWait(), maxBackoff())
                .withJitter(jitter() ? 0.5 : 0.0)
                .atMost(retryLimit())
                .onItem().invoke(o -> {
                    if (debug()) {
                        System.out.printf("Blocking Step %s streamed item: %s%n",
                                this.getClass().getSimpleName(), o);
                    }
                });
        });
    }
}