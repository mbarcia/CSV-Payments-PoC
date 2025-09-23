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

package io.github.mbarcia.pipeline;

import io.github.mbarcia.pipeline.config.LiveStepConfig;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.step.*;
import io.github.mbarcia.pipeline.step.functional.ManyToMany;
import io.github.mbarcia.pipeline.step.functional.ManyToOne;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Executors;

@ApplicationScoped
public class PipelineRunner implements AutoCloseable {

    @Inject
    ConfigFactory configFactory;

    @Inject
    PipelineConfig pipelineConfig;

    private final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Run a pipeline: input Multi through the list of steps.
     */
    public Object run(Multi<?> input, List<Object> steps) {
        Object current = input;

        for (Object step : steps) {
            if (step instanceof Configurable c) {
                LiveStepConfig live = configFactory.buildLiveConfig(step.getClass(), pipelineConfig);
                c.initialiseWithConfig(live);
            }

            switch (step) {
                case StepOneToOne<?, ?> stepOneToOne -> current = applyOneToOneUnchecked(stepOneToOne, current);
                case StepOneToMany<?, ?> stepOneToMany -> current = applyOneToManyUnchecked(stepOneToMany, current);
                case ManyToOne<?, ?> manyToOne -> current = applyManyToOneUnchecked(manyToOne, current);
                case ManyToMany<?, ?> manyToMany -> current = applyManyToManyUnchecked(manyToMany, current);
                case null, default -> System.err.println("Step not recognised");
            }
        }

        return current; // could be Uni<?> or Multi<?>
    }

    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
    private static <I, O> Object applyOneToOneUnchecked(io.github.mbarcia.pipeline.step.StepOneToOne<I, O> step, Object current) {
        if (current instanceof Uni<?>) {
            return step.apply((Uni<I>) current);
        } else if (current instanceof Multi<?>) {
            // Apply the OneToOne step to each item in the Multi individually
            Multi<O> result = ((Multi<I>) current)
                .onItem()
                .transformToUni(step::applyOneToOne)
                .concatenate();
            return result;
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported current type for OneToOne: {0}", current));
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