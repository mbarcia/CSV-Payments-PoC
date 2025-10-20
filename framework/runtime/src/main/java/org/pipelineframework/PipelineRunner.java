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
import java.util.concurrent.Executors;
import org.jboss.logging.Logger;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.PipelineStepsConfig;
import org.pipelineframework.step.*;
import org.pipelineframework.step.functional.ManyToMany;
import org.pipelineframework.step.functional.ManyToOne;

@ApplicationScoped
@Unremovable
public class PipelineRunner implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(PipelineRunner.class);

    @Inject
    ConfigFactory configFactory;

    @Inject
    PipelineConfig pipelineConfig;

    private final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Execute the configured pipeline steps against the provided Multi input.
     *
     * @param input the source Multi of items to process through the pipeline
     * @return the pipeline's resulting reactive stream — either a `Multi<?>` or a `Uni<?>` representing the final stage
     */
    public Object run(Multi<?> input) {
        List<Object> steps = loadPipelineSteps();
        
        if (steps.isEmpty()) {
            LOG.warn("No steps available to execute - pipeline will not process data");
        }
        
        return run(input, steps);
    }

    /**
     * Execute the pipeline by applying the ordered steps to the given input stream.
     *
     * <p>Each non-null step from {@code steps} is applied in sequence to the current pipeline state.
     * If a step implements {@code Configurable} it will be initialised with a live configuration
     * before being applied. Unknown step types are ignored (logged) and null entries are skipped.</p>
     *
     * @param input  the initial Multi stream to be processed
     * @param steps  an ordered list of pipeline step instances; null entries are skipped and
     *               {@code Configurable} steps are initialised before application
     * @return       the resulting pipeline output, typically a {@code Uni<?>} or {@code Multi<?>} representing
     *               the pipeline's final stream
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object run(Multi<?> input, List<Object> steps) {
        Object current = input;

        for (Object step : steps) {
            if (step == null) {
                LOG.warn("Warning: Found null step in configuration, skipping...");
                continue;
            }
            
            if (step instanceof Configurable c) {
                try {
                   c.initialiseWithConfig(configFactory.buildLiveConfig(step.getClass(), pipelineConfig));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not initialise step " + c , e);
                }
            }

            Class<?> clazz = step.getClass();
            LOG.debugf("Step class: %s", clazz.getName());
            for (Class<?> iface : clazz.getInterfaces()) {
                LOG.debugf("Implements: %s", iface.getName());
            }

            switch (step) {
                case StepOneToOne stepOneToOne -> current = applyOneToOneUnchecked(stepOneToOne, current);
                case StepOneToMany stepOneToMany -> current = applyOneToManyUnchecked(stepOneToMany, current);
                case org.pipelineframework.step.blocking.StepOneToManyBlocking stepOneToManyBlocking -> current = applyOneToManyBlockingUnchecked(stepOneToManyBlocking, current);
                case org.pipelineframework.step.future.StepOneToOneCompletableFuture stepFuture -> current = applyOneToOneFutureUnchecked(stepFuture, current);
                case ManyToOne manyToOne -> current = applyManyToOneUnchecked(manyToOne, current);
                case ManyToMany manyToMany -> current = applyManyToManyUnchecked(manyToMany, current);
                default -> LOG.errorf("Step not recognised: %s", step.getClass().getName());
            }
        }

        return current; // could be Uni<?> or Multi<?>
    }

    /**
     * Load and instantiate pipeline steps defined in application properties.
     *
     * <p>Reads configured properties for a predefined set of step names (properties
     * under `pipeline.steps.{name}`), collects each step's `class` and optional
     * `type` and `order` properties, instantiates managed step objects via
     * {@code createStepFromConfig}, sorts steps by the numeric `order` property
     * (steps without a valid `order` default to 100), and returns the instantiated
     * steps in execution order. If configuration cannot be read, returns an empty list.
     *
     * @return a list of instantiated pipeline step objects in execution order
     */
    private List<Object> loadPipelineSteps() {
        try {
            // Use the structured configuration mapping to get all pipeline steps
            PipelineStepsConfig pipelineStepsConfig = CDI.current()
                .select(PipelineStepsConfig.class).get();

            Map<String, org.pipelineframework.config.PipelineStepsConfig.StepConfig> stepConfigs =
                pipelineStepsConfig.steps();

            // Sort the steps by their order property
            List<Map.Entry<String, org.pipelineframework.config.PipelineStepsConfig.StepConfig>> sortedStepEntries =
                stepConfigs.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(
                        Comparator.comparingInt(org.pipelineframework.config.PipelineStepsConfig.StepConfig::order)))
                    .toList();

            List<Object> steps = new ArrayList<>();
            for (Map.Entry<String, org.pipelineframework.config.PipelineStepsConfig.StepConfig> entry : sortedStepEntries) {
                String stepName = entry.getKey();
                org.pipelineframework.config.PipelineStepsConfig.StepConfig stepConfig = entry.getValue();
                Object step = createStepFromConfig(stepName, stepConfig);
                if (step != null) {
                    steps.add(step);
                }
            }

            LOG.debugf("Loaded %d pipeline steps from application properties", steps.size());
            return steps;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load configuration: %s", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Instantiate a pipeline step from its configuration and return a CDI-managed instance.
     *
     * @param stepName the logical name of the step (used only for error logging)
     * @param config the step configuration containing the class name and other properties
     * @return a CDI-managed instance of the configured class, or `null` if the `class` key is missing or instantiation fails
     */
    private Object createStepFromConfig(String stepName, org.pipelineframework.config.PipelineStepsConfig.StepConfig config) {
        String stepClassName = config.className();
        if (stepClassName == null) {
            LOG.errorf("No class specified for pipeline step: %s", stepName);
            return null;
        }

        try {
            Class<?> stepClass = Thread.currentThread().getContextClassLoader().loadClass(stepClassName);
            return Arc.container().instance(stepClass).get();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to instantiate pipeline step: %s, error: %s", stepClassName, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <I, O> Object applyOneToOneUnchecked(org.pipelineframework.step.StepOneToOne<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            if (step.parallel()) {
                return ((Multi<I>) current).flatMap(item -> step.apply(Uni.createFrom().item(item)).toMulti());
            } else {
                return ((Multi<I>) current).concatMap(item -> step.apply(Uni.createFrom().item(item)).toMulti());
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for OneToOne: {0}", current));
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <I, O> Object applyOneToManyBlockingUnchecked(org.pipelineframework.step.blocking.StepOneToManyBlocking<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            if (step.parallel()) {
                return ((Multi<I>) current).flatMap(item -> step.apply(Uni.createFrom().item(item)));
            } else {
                return ((Multi<I>) current).concatMap(item -> step.apply(Uni.createFrom().item(item)));
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for OneToManyBlocking: {0}", current));
        }
    }

    @SuppressWarnings({"unchecked"})
    private static <I, O> Object applyOneToManyUnchecked(org.pipelineframework.step.StepOneToMany<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            if (step.parallel()) {
                return ((Multi<I>) current).flatMap(item -> step.apply(Uni.createFrom().item(item)));
            } else {
                return ((Multi<I>) current).concatMap(item -> step.apply(Uni.createFrom().item(item)));
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for OneToManyBlocking: {0}", current));
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, O> Object applyManyToOneUnchecked(ManyToOne<I, O> step, Object current) {
        if (current instanceof Multi<?>) {
            return step.applyReduce((Multi<I>) current);
        } else if (current instanceof Uni<?>) {
            // convert Uni to Multi and call applyReduce
            return step.applyReduce(((Uni<I>) current).toMulti());
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for ManyToOne: {0}", current));
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, O> Object applyOneToOneFutureUnchecked(org.pipelineframework.step.future.StepOneToOneCompletableFuture<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            if (step.parallel()) {
                return ((Multi<I>) current).flatMap(item -> step.apply(Uni.createFrom().item(item)).toMulti());
            } else {
                return ((Multi<I>) current).concatMap(item -> step.apply(Uni.createFrom().item(item)).toMulti());
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for OneToOneCompletableFuture: {0}", current));
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, O> Object applyManyToManyUnchecked(ManyToMany<I, O> step, Object current) {
        if (current instanceof Multi<?>) {
            return step.apply((Multi<I>) current);
        } else if (current instanceof Uni<?>) {
            return step.apply(((Uni<I>) current).toMulti());
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for ManyToMany: {0}", current));
        }
    }

    @Override
    public void close() {
        if (vThreadExecutor instanceof AutoCloseable ac) {
            try { ac.close(); } catch (Exception ignored) {}
        }
    }
}