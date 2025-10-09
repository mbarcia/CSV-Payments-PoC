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

package io.github.mbarcia.pipeline.step.blocking;

import io.github.mbarcia.pipeline.step.Configurable;
import io.github.mbarcia.pipeline.step.DeadLetterQueue;
import io.github.mbarcia.pipeline.step.functional.OneToMany;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imperative variant of StepOneToMany that works with Lists instead of Multi.
 * <p>
 * This interface is designed for developers who prefer imperative programming
 * and want to work with standard Java collections instead of reactive streams.
 * <p>
 * The PipelineRunner will automatically handle the conversion between reactive
 * and imperative representations.
 */
public interface StepOneToManyBlocking<I, O> extends Configurable, OneToMany<I, O>, DeadLetterQueue<I, O> {

    List<O> applyList(I in);

    default int concurrency() { return 1; }

    default boolean runWithVirtualThreads() { return false; }

    @Override
    default Multi<O> apply(Uni<I> inputUni) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        final Executor executor = runWithVirtualThreads() ? vThreadExecutor : null;

        return inputUni
            .onItem().transformToMulti(item -> {
                Multi<O> multi = Multi.createFrom().iterable(() -> {
                    try {
                        return applyList(item).iterator();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                if (executor != null) {
                    multi = multi.runSubscriptionOn(executor);
                }

                return multi.onItem().invoke(o -> {
                    if (debug()) {
                        LOG.debug("Blocking Step {} emitted item: {}", this.getClass().getSimpleName(), o);
                    }
                });
            });
    }}