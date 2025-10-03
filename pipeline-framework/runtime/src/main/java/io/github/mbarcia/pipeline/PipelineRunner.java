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

package io.github.mbarcia.pipeline;

import io.github.mbarcia.pipeline.config.LiveStepConfig;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.step.*;
import io.github.mbarcia.pipeline.step.functional.ManyToMany;
import io.github.mbarcia.pipeline.step.functional.ManyToOne;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

@ApplicationScoped
@Unremovable
public class PipelineRunner implements AutoCloseable {

    @Inject
    ConfigFactory configFactory;

    @Inject
    PipelineConfig pipelineConfig;

    @Inject
    Instance<StepsRegistry> stepsRegistryInstance;

    private final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Run a pipeline: input Multi through the list of steps from the registry.
     */
    public Object run(Multi<?> input) {
        List<Object> steps;
        if (stepsRegistryInstance.isResolvable()) {
            StepsRegistry registry = stepsRegistryInstance.get();
            System.out.println("StepsRegistry injected successfully: " + registry.getClass().getName());
            List<Object> allSteps = registry.getSteps();
            System.out.println("Total steps from registry: " + allSteps.size());
            
            // Filter out any null steps that may have been injected
            steps = allSteps.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("Non-null steps: " + steps.size());
            int nullCount = allSteps.size() - steps.size();
            if (nullCount > 0) {
                System.out.println("Found " + nullCount + " null steps in registry");
            }
        } else {
            System.out.println("StepsRegistry is not resolvable, using empty list");
            steps = java.util.Collections.emptyList();
        }
        
        if (steps.isEmpty()) {
            System.out.println("No steps available to execute - pipeline will not process data");
        }
        
        return run(input, steps);
    }
    
    /**
     * Run a pipeline: input Multi through the provided list of steps.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object run(Multi<?> input, List<Object> steps) {
        Object current = input;

        for (Object step : steps) {
            if (step == null) {
                System.err.println("Warning: Found null step in registry, skipping...");
                continue;
            }
            
            if (step instanceof Configurable c) {
                LiveStepConfig live = configFactory.buildLiveConfig(step.getClass(), pipelineConfig);
                c.initialiseWithConfig(live);
            }

            Class<?> clazz = step.getClass();
            System.out.println("Step class: " + clazz.getName());
            for (Class<?> iface : clazz.getInterfaces()) {
                System.out.println("Implements: " + iface.getName());
            }

            switch (step) {
                case StepOneToOne stepOneToOne -> current = applyOneToOneUnchecked(stepOneToOne, current);
                case StepOneToMany stepOneToMany -> current = applyOneToManyUnchecked(stepOneToMany, current);
                case io.github.mbarcia.pipeline.step.blocking.StepOneToManyBlocking stepOneToManyBlocking -> current = applyOneToManyBlockingUnchecked(stepOneToManyBlocking, current);
                case io.github.mbarcia.pipeline.step.future.StepOneToOneCompletableFuture stepFuture -> current = applyOneToOneFutureUnchecked(stepFuture, current);
                case ManyToOne manyToOne -> current = applyManyToOneUnchecked(manyToOne, current);
                case ManyToMany manyToMany -> current = applyManyToManyUnchecked(manyToMany, current);
                default -> System.err.println("Step not recognised: " + step.getClass().getName());
            }
        }

        return current; // could be Uni<?> or Multi<?>
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    private static <I, O> Object applyOneToOneUnchecked(io.github.mbarcia.pipeline.step.StepOneToOne<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            // Apply the OneToOne step to each item in the Multi, using the apply method 
            // which includes retry and recovery logic
            Multi<O> result = ((Multi<I>) current)
                .onItem()
                .transformToUni(item -> step.apply(Uni.createFrom().item(item)))
                .concatenate();
            return result;
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for OneToOne: {0}", current));
        }
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    private static <I, O> Object applyOneToManyBlockingUnchecked(io.github.mbarcia.pipeline.step.blocking.StepOneToManyBlocking<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            // Apply the OneToMany blocking step to each item in the Multi, producing a Multi of Multi<O>,
            // then flatten it to a single Multi<O>
            Multi<O> result = ((Multi<I>) current)
                .onItem()
                .transformToMultiAndConcatenate(item -> step.apply(Uni.createFrom().item(item)));
            return result;
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for OneToManyBlocking: {0}", current));
        }
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    private static <I, O> Object applyOneToManyUnchecked(io.github.mbarcia.pipeline.step.StepOneToMany<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            // Apply the OneToMany step to each item in the Multi, producing a Multi of Multi<O>,
            // then flatten it to a single Multi<O>
            Multi<O> result = ((Multi<I>) current)
                .onItem()
                .transformToMulti(step::applyOneToMany)
                .concatenate();
            return result;
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for OneToMany: {0}", current));
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
    private static <I, O> Object applyOneToOneFutureUnchecked(io.github.mbarcia.pipeline.step.future.StepOneToOneCompletableFuture<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            // Apply the OneToOne step to each item in the Multi, using the apply method 
            // which includes retry and recovery logic
            Multi<O> result = ((Multi<I>) current)
                .onItem()
                .transformToUni(item -> step.apply(Uni.createFrom().item(item)))
                .concatenate();
            return result;
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