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

package org.pipelineframework;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.*;
import org.jboss.logging.Logger;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.step.*;
import org.pipelineframework.step.blocking.StepOneToManyBlocking;
import org.pipelineframework.step.functional.ManyToOne;
import org.pipelineframework.step.future.StepOneToOneCompletableFuture;

/**
 * A service that runs a sequence of pipeline steps against a reactive source.
 *
 * This class orchestrates the execution of pipeline steps, handling the transformation of reactive streams
 * through various step types (one-to-one, one-to-many, many-to-one, many-to-many).
 */
@ApplicationScoped
@Unremovable
public class PipelineRunner implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(PipelineRunner.class);

    /**
     * Constructs a new PipelineRunner instance.
     */

    @Inject
    ConfigFactory configFactory;

    @Inject
    PipelineConfig pipelineConfig;

    /**
     * Run a sequence of pipeline steps against the provided reactive source.
     *
     * Configurable steps are initialised with configuration built from the injected factories before being applied.
     *
     * @param input the source Multi of items to process through the pipeline; may be transformed to a Uni/Multi by steps
     * @param steps the ordered list of step instances to apply; must not be null; null entries are skipped
     * @return either a Multi containing the resulting stream of items or a Uni containing the final single result
     * @throws NullPointerException if steps is null
     */
    public Object run(Multi<?> input, List<Object> steps) {
        Objects.requireNonNull(steps, "Steps list must not be null");
        Object current = input;

        for (Object step : steps) {
            if (step == null) {
                logger.warn("Warning: Found null step in configuration, skipping...");
                continue;
            }

            if (step instanceof Configurable c) {
               c.initialiseWithConfig(configFactory.buildConfig(step.getClass(), pipelineConfig));
            }

            Class<?> clazz = step.getClass();
            logger.debugf("Step class: %s", clazz.getName());
            for (Class<?> iface : clazz.getInterfaces()) {
                logger.debugf("Implements: %s", iface.getName());
            }

            switch (step) {
                case StepOneToOne stepOneToOne -> current = applyOneToOneUnchecked(stepOneToOne, current);
                case StepOneToOneCompletableFuture stepFuture -> current = applyOneToOneFutureUnchecked(stepFuture, current);
                case StepOneToMany stepOneToMany -> current = applyOneToManyUnchecked(stepOneToMany, current);
                case StepOneToManyBlocking stepOneToManyBlocking -> current = applyOneToManyBlockingUnchecked(stepOneToManyBlocking, current);
                case ManyToOne manyToOne -> current = applyManyToOneUnchecked(manyToOne, current);
                case StepManyToMany manyToMany -> current = applyManyToManyUnchecked(manyToMany, current);
                default -> logger.errorf("Step not recognised: %s", step.getClass().getName());
            }
        }

        return current; // could be Uni<?> or Multi<?>
    }

    /**
     * Apply a one-to-one pipeline step to the provided reactive stream and produce the transformed stream.
     *
     * @param <I>     the input type of the step
     * @param <O>     the output type of the step
     * @param step    the step that transforms items of type I to type O
     * @param current a Uni&lt;?&gt; or Multi&lt;?&gt; that provides the input items; other types are not supported
     * @return        the resulting Uni&lt;?&gt; or Multi&lt;?&gt; after applying the step
     * @throws IllegalArgumentException if {@code current} is neither a Uni&lt;?&gt; nor a Multi&lt;?&gt;
     */
    @SuppressWarnings({"unchecked"})
    public static <I, O> Object applyOneToOneUnchecked(StepOneToOne<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            if (step.parallel()) {
                logger.debugf("Applying step %s (flatMap)", step.getClass());
                return ((Multi<I>) current).flatMap(item -> step.apply(Uni.createFrom().item(item)).toMulti());
            } else {
                logger.debugf("Applying step %s (concatMap)", step.getClass());
                return ((Multi<I>) current).concatMap(item -> step.apply(Uni.createFrom().item(item)).toMulti());
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for StepOneToOne: {0}", current));
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, O> Object applyOneToOneFutureUnchecked(StepOneToOneCompletableFuture<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            if (step.parallel()) {
                return ((Multi<I>) current).flatMap(item -> step.apply(Uni.createFrom().item(item)).toMulti());
            } else {
                return ((Multi<I>) current).concatMap(item -> step.apply(Uni.createFrom().item(item)).toMulti());
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for StepOneToOneCompletableFuture: {0}", current));
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <I, O> Object applyOneToManyBlockingUnchecked(StepOneToManyBlocking<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            if (step.parallel()) {
                logger.debugf("Applying step %s (flatMap)", step.getClass());
                return ((Multi<I>) current).flatMap(item -> step.apply(Uni.createFrom().item(item)));
            } else {
                logger.debugf("Applying step %s (concatMap)", step.getClass());
                return ((Multi<I>) current).concatMap(item -> step.apply(Uni.createFrom().item(item)));
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for StepOneToManyBlocking: {0}", current));
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <I, O> Object applyOneToManyUnchecked(StepOneToMany<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            if (step.parallel()) {
                logger.debugf("Applying step %s (flatMap)", step.getClass());
                return ((Multi<I>) current).flatMap(item -> step.apply(Uni.createFrom().item(item)));
            } else {
                logger.debugf("Applying step %s (concatMap)", step.getClass());
                return ((Multi<I>) current).concatMap(item -> step.apply(Uni.createFrom().item(item)));
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for StepOneToMany: {0}", current));
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, O> Object applyManyToOneUnchecked(ManyToOne<I, O> step, Object current) {
        if (current instanceof Multi<?>) {
            return step.apply((Multi<I>) current);
        } else if (current instanceof Uni<?>) {
            // convert Uni to Multi and call apply
            return step.apply(((Uni<I>) current).toMulti());
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for StepManyToOne: {0}", current));
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, O> Object applyManyToManyUnchecked(StepManyToMany<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            // Single async source — convert to Multi and process
            return step.apply(((Uni<I>) current).toMulti());
        } else if (current instanceof Multi<?> c) {
            logger.debugf("Applying many-to-many step %s on full stream", step.getClass());
            // ✅ Just pass the whole stream to the step
            return step.apply((Multi<I>) c);
        } else {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Unsupported current type for StepManyToMany: {0}", current));
        }
    }

    /**
     * Performs no action; PipelineRunner has no resources to release on close.
     */
    @Override
    public void close() {
    }
}