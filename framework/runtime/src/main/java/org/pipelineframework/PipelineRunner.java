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
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executors;
import org.jboss.logging.Logger;
import org.pipelineframework.config.LiveStepConfig;
import org.pipelineframework.config.PipelineConfig;
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
                LiveStepConfig live = configFactory.buildLiveConfig(step.getClass(), pipelineConfig);
                c.initialiseWithConfig(live);
            }

            Class<?> clazz = step.getClass();
            LOG.infof("Step class: %s", clazz.getName());
            for (Class<?> iface : clazz.getInterfaces()) {
                LOG.infof("Implements: %s", iface.getName());
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
        // Get all configured pipeline steps from application properties
        Map<String, Map<String, String>> stepConfigs = new HashMap<>();
        
        try {
            // Access Quarkus configuration properties
            org.eclipse.microprofile.config.Config config = 
                org.eclipse.microprofile.config.ConfigProvider.getConfig();
            
            // Since we can't iterate safely, let's try to get specific known properties
            // The configuration properties will be in the form: pipeline.steps.{name}.{property}
            // We'll need to know the step names in advance or use a different strategy
            
            // A better approach: use Quarkus Config's ability to create configuration objects
            // For now, let's look for a known property to see if configuration access works
            Optional<String> testProp = config.getOptionalValue("pipeline.steps.process-folder.class", String.class);
            testProp.ifPresent(s -> LOG.debugf("Found test property: %s", s));
            
            // Instead of iterating all properties, let's use the fact that we know the pattern
            // We'll try to get properties for known step names (this could be made more dynamic)
            String[] stepNames = {"process-folder", "process-csv-input", "send-payment", "process-ack-payment", "process-payment-status", "process-csv-output"};
            
            for (String stepName : stepNames) {
                String classProp = "pipeline.steps." + stepName + ".class";
                String typeProp = "pipeline.steps." + stepName + ".type";
                String orderProp = "pipeline.steps." + stepName + ".order";
                
                Optional<String> className = config.getOptionalValue(classProp, String.class);
                if (className.isPresent()) {
                    Map<String, String> stepConfig = new HashMap<>();
                    stepConfig.put("class", className.get());
                    
                    config.getOptionalValue(typeProp, String.class).ifPresent(type -> stepConfig.put("type", type));
                    config.getOptionalValue(orderProp, String.class).ifPresent(order -> stepConfig.put("order", order));
                    
                    stepConfigs.put(stepName, stepConfig);
                }
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load configuration: %s", e.getMessage());
            return Collections.emptyList();
        }

        // Sort the steps by a potential order property if present
        List<String> sortedStepNames = stepConfigs.keySet().stream()
            .sorted(Comparator.comparingInt(name -> {
                Map<String, String> config = stepConfigs.get(name);
                String orderStr = config.getOrDefault("order", "100");
                try {
                    return Integer.parseInt(orderStr);
                } catch (NumberFormatException e) {
                    return 100; // default order
                }
            }))
            .toList();

        List<Object> steps = new ArrayList<>();
        for (String stepName : sortedStepNames) {
            Map<String, String> stepConfig = stepConfigs.get(stepName);
            Object step = createStepFromConfig(stepName, stepConfig);
            if (step != null) {
                steps.add(step);
            }
        }

        LOG.infof("Loaded %d pipeline steps from application properties", steps.size());
        return steps;
    }

    /**
     * Instantiate a pipeline step from its configuration and return a CDI-managed instance.
     *
     * @param stepName the logical name of the step (used only for error logging)
     * @param config a map of step properties; must include the `class` key with the step's fully-qualified class name
     * @return a CDI-managed instance of the configured class, or `null` if the `class` key is missing or instantiation fails
     */
    private Object createStepFromConfig(String stepName, Map<String, String> config) {
        String stepClassName = config.get("class");
        if (stepClassName == null) {
            LOG.errorf("No class specified for pipeline step: %s", stepName);
            return null;
        }

        try {
            Class<?> stepClass = Thread.currentThread().getContextClassLoader().loadClass(stepClassName);
            return CDI.current().select(stepClass).get();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to instantiate pipeline step: %s, error: %s", stepClassName, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    private static <I, O> Object applyOneToOneUnchecked(org.pipelineframework.step.StepOneToOne<I, O> step, Object current) {
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
    private static <I, O> Object applyOneToManyBlockingUnchecked(org.pipelineframework.step.blocking.StepOneToManyBlocking<I, O> step, Object current) {
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
    private static <I, O> Object applyOneToManyUnchecked(org.pipelineframework.step.StepOneToMany<I, O> step, Object current) {
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
    private static <I, O> Object applyOneToOneFutureUnchecked(org.pipelineframework.step.future.StepOneToOneCompletableFuture<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            // Apply the OneToOne step to each item in the Multi, using the apply method 
            // which includes retry and recovery logic
            return ((Multi<I>) current)
                .onItem()
                .transformToUni(item -> step.apply(Uni.createFrom().item(item)))
                .concatenate();
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