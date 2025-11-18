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

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.*;
import org.jboss.logging.Logger;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.PipelineStepConfig;
import org.pipelineframework.step.*;
import org.pipelineframework.step.blocking.StepOneToManyBlocking;
import org.pipelineframework.step.functional.ManyToOne;
import org.pipelineframework.step.future.StepOneToOneCompletableFuture;

@ApplicationScoped
@Unremovable
public class PipelineRunner implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(PipelineRunner.class);

    @Inject
    ConfigFactory configFactory;

    @Inject
    PipelineConfig pipelineConfig;


    /**
     * Execute the configured pipeline steps against the provided Multi input.
     *
     * @param input the source Multi of items to process through the pipeline
     * @return the pipeline's resulting reactive stream — either a `Multi<?>` or a `Uni<?>` representing the final stage
     */
    public Object run(Multi<?> input) {
        List<Object> steps = loadPipelineSteps();

        if (steps.isEmpty()) {
            logger.warn("No steps available to execute - pipeline will not process data");
        }

        return run(input, steps);
    }

    /**
     * Apply an ordered sequence of pipeline steps to the provided input stream.
     *
     * <p>Each non-null entry in {@code steps} is applied in sequence to the pipeline state.
     * Steps that implement {@code Configurable} are initialised with a configuration built
     * from the injected {@code configFactory} and {@code pipelineConfig} before application.
     * Null entries are skipped and unrecognised step types are logged and ignored.</p>
     *
     * @param input the initial Multi stream to be processed
     * @param steps an ordered list of pipeline step instances; null entries are skipped and
     *              {@code Configurable} steps are initialised prior to being applied
     * @return      the resulting pipeline output, typically a {@code Uni<?>} or {@code Multi<?>}
     * @throws RuntimeException if initialisation of a {@code Configurable} step fails due to access restrictions
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object run(Multi<?> input, List<Object> steps) {
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
     * Load and instantiate pipeline steps configured via PipelineStepConfig.
     *
     * <p>Reads the mapped step configurations, instantiates CDI-managed step objects for each
     * configured entry, and returns them in execution order. Steps are ordered by their
     * `order` property; entries without an `order` are treated as 100. If the configuration
     * cannot be read or an error occurs, an empty list is returned.
     *
     * @return the instantiated pipeline step objects in execution order, or an empty list if configuration cannot be read
     */
    private List<Object> loadPipelineSteps() {
        try {
            // Use the structured configuration mapping to get all pipeline steps
            PipelineStepConfig pipelineStepConfig = CDI.current()
                .select(PipelineStepConfig.class).get();

            Map<String, org.pipelineframework.config.PipelineStepConfig.StepConfig> stepConfigs =
                pipelineStepConfig.step();

            // Sort the steps by their order property
            List<Map.Entry<String, org.pipelineframework.config.PipelineStepConfig.StepConfig>> sortedStepEntries =
                stepConfigs.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(
                        Comparator.comparingInt(config -> config.order() != null ? config.order() : 100)))
                    .toList();

            List<Object> steps = new ArrayList<>();
            for (Map.Entry<String, org.pipelineframework.config.PipelineStepConfig.StepConfig> entry : sortedStepEntries) {
                String stepClassName = entry.getKey();  // The fully qualified class name
                org.pipelineframework.config.PipelineStepConfig.StepConfig stepConfig = entry.getValue();
                Object step = createStepFromConfig(stepClassName, stepConfig);
                if (step != null) {
                    steps.add(step);
                }
            }

            logger.debugf("Loaded %d pipeline steps from application properties", steps.size());
            return steps;
        } catch (Exception e) {
            logger.errorf(e, "Failed to load configuration: %s", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Instantiate a pipeline step from its configuration and return a CDI-managed instance.
     *
     * @param stepClassName the fully qualified class name of the step
     * @param config the step configuration containing other properties
     * @return a CDI-managed instance of the configured class, or `null` if instantiation fails
     */
    private Object createStepFromConfig(String stepClassName, org.pipelineframework.config.PipelineStepConfig.StepConfig config) {
        try {
            Class<?> stepClass = Thread.currentThread().getContextClassLoader().loadClass(stepClassName);
            Object step = Arc.container().instance(stepClass).get();
            return step;
        } catch (Exception e) {
            logger.errorf(e, "Failed to instantiate pipeline step: %s, error: %s", stepClassName, e.getMessage());
            return null;
        }
    }

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