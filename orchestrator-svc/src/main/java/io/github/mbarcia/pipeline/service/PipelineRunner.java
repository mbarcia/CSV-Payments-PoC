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

package io.github.mbarcia.pipeline.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineRunner implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunner.class);

    private final Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Run a pipeline: input Multi through the list of steps.
     */
    @SuppressWarnings("unchecked")
    public Multi<Object> run(Multi<?> input, List<? extends StepBase> steps) {
        Multi<Object> current = (Multi<Object>) input;

        for (StepBase step : steps) {
            current = applyStep(current, step);
        }

        return current;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Multi<Object> applyStep(Multi<Object> input, StepBase step) {
        switch (step) {
            case StepOneToOne s -> {
                return input.onItem().transformToUniAndMerge(item ->
                        applyWithRetries(() -> Uni.createFrom().item(s.apply(item))
                                .runSubscriptionOn(vThreadExecutor), item, s
                        )
                );
            }
            case StepOneToMany s -> {
                Executor executor = s.effectiveConfig().runWithVirtualThreads() ? vThreadExecutor : null;
                int concurrency = Math.max(1, s.effectiveConfig().concurrency());

                return input.onItem().transformToMulti(item -> {
                    Multi<Object> multi = s.applyMulti(item);

                    if (executor != null) {
                        // shift blocking subscription work to vthreads
                        multi = multi.runSubscriptionOn(executor);
                    }

                    //noinspection ReactiveStreamsUnusedPublisher
                    return multi.onItem().transform(o -> {
                        if (s.debug()) {
                            LOG.debug(
                                    "Step {0} emitted item: {}{}",
                                    s.getClass().getSimpleName(), o
                            );
                        }
                        return o;
                    });
                }).merge(concurrency);
            }
            case StepManyToMany s -> {
                Executor executor = s.effectiveConfig().runWithVirtualThreads() ? vThreadExecutor : null;

                return Multi.createFrom().deferred(() -> {
                    Multi<Object> baseUpstream = (executor != null)
                            ? input.runSubscriptionOn(executor)
                            : input;

                    Multi<Object> out = s.applyStreaming(baseUpstream);

                    return out
                        .onFailure().retry()
                        .withBackOff(s.retryWait(), s.maxBackoff())
                        .withJitter(s.jitter() ? 0.5 : 0.0)
                        .atMost(s.retryLimit())
                        .onFailure().recoverWithMulti(err -> {
                            if (s.recoverOnFailure()) {
                                if (s.debug()) {
                                    System.err.printf("Step %s failed streaming: %s%n",
                                            s.getClass().getSimpleName(), err);
                                }
                                return s.deadLetterMulti(baseUpstream, err); // custom DLQ handling
                            } else {
                                return Multi.createFrom().failure(err);
                            }
                        })
                        .onItem().invoke(o -> {
                            if (s.debug()) {
                                System.out.printf("Step %s streamed item: %s%n",
                                        s.getClass().getSimpleName(), o);
                            }
                        })
                        .onCompletion().invoke(() -> {
                            if (s.debug()) {
                                System.out.printf("Step %s completed streaming%n",
                                        s.getClass().getSimpleName());
                            }
                        });
                });
            }
            case StepOneToAsync s -> {
                return input.onItem().transformToUniAndMerge(item ->
                        applyWithRetries(() -> s.applyAsyncUni(item), item, s)
                );
            }
            case null, default -> throw new IllegalArgumentException(MessageFormat.format("Unknown step type: {0}", step != null ? step.getClass() : "null step"));
        }
    }

    /**
     * Apply retries using Mutiny built-in backoff + jitter.
     */
    private Uni<Object> applyWithRetries(Supplier<Uni<Object>> uniSupplier, Object item, StepBase step) {
        try {
            Uni<Object> uni = uniSupplier.get();
            if (uni == null) {
                return Uni.createFrom().failure(new NullPointerException("Step returned null Uni"));
            }

            return uni.onFailure().retry()
                    .withBackOff(step.retryWait(), step.maxBackoff())
                    .withJitter(step.jitter() ? 0.5 : 0.0)
                    .atMost(step.retryLimit())
                    .onFailure().recoverWithUni(err -> {
                        if (step.recoverOnFailure()) {
                            if (step.debug()) {
                                LOG.debug(
                                        "Step {0}: failed item={} after {} retries: {}",
                                        step.getClass().getSimpleName(), item, step.retryLimit(), err
                                );
                            }
                            return step.deadLetter(item, err).replaceWith(item);
                        } else {
                            return Uni.createFrom().failure(err);
                        }
                    })
                    .onItem().invoke(i -> {
                        if (step.debug()) {
                            LOG.debug(
                                    "Step {} processed item: {}",
                                    step.getClass().getSimpleName(), i
                            );
                        }
                    })
                    .onTermination().invoke(() -> {
                        // Termination handler
                    });

        } catch (Throwable t) {
            if (step.recoverOnFailure()) {
                if (step.debug()) {
                    LOG.debug(
                            "Step {0}: synchronous failure item={}: {}",
                            step.getClass().getSimpleName(), item, t
                    );
                }
                return step.deadLetter(item, t).replaceWith(item);
            } else {
                return Uni.createFrom().failure(t);
            }
        }
    }

    @Override
    public void close() {
        if (vThreadExecutor instanceof AutoCloseable ac) {
            try { ac.close(); } catch (Exception ignored) {}
        }
    }
}