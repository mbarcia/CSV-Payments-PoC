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

package org.pipelineframework.step;

import io.smallrye.mutiny.Multi;
import java.util.concurrent.Executors;
import org.pipelineframework.step.functional.ManyToMany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** N -> N */
public interface StepManyToMany<I, O> extends Configurable, ManyToMany<I, O>, DeadLetterQueue<I, O> {
    Multi<O> applyTransform(Multi<I> input);

	@Override
    default Multi<O> apply(Multi<I> input) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        java.util.concurrent.Executor executor = effectiveConfig().runWithVirtualThreads() ? vThreadExecutor : null;

        // Apply the transformation
        Multi<O> output = applyTransform(input);

        // Apply overflow strategy
        if ("buffer".equalsIgnoreCase(backpressureStrategy())) {
            output = output.onOverflow().buffer(backpressureBufferCapacity());
        } else if ("drop".equalsIgnoreCase(backpressureStrategy())) {
            output = output.onOverflow().drop();
        } else {
            // default behavior - buffer with default capacity
            output = output.onOverflow().buffer(128); // default buffer size
        }

        if (executor != null) {
            // shift blocking subscription work to virtual threads
            output = output.runSubscriptionOn(executor);
        }

        return output.onItem().transform(item -> {
            if (debug()) {
                LOG.debug(
                    "Step {} emitted item: {}",
                    this.getClass().getSimpleName(), item
                );
            }
            return item;
        })
        .onFailure(t -> !(t instanceof NullPointerException)).retry()
        .withBackOff(retryWait(), maxBackoff())
        .withJitter(jitter() ? 0.5 : 0.0)
        .atMost(retryLimit());
    }
}