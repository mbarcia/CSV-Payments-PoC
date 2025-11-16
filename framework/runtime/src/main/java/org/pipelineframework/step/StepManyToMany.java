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
import org.jboss.logging.Logger;
import org.pipelineframework.step.functional.ManyToMany;

/** N -> N */
public interface StepManyToMany<I, O> extends Configurable, ManyToMany<I, O>, DeadLetterQueue<I, O> {
    Multi<O> applyTransform(Multi<I> input);

	@Override
    default Multi<O> apply(Multi<I> input) {
        final Logger LOG = Logger.getLogger(this.getClass());

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

        return output.onItem().transform(item -> {
            LOG.debugf(
                "Step %s emitted item: %s",
                this.getClass().getSimpleName(), item
            );
            return item;
        })
        .onFailure(t -> !(t instanceof NullPointerException)).retry()
        .withBackOff(retryWait(), maxBackoff())
        .withJitter(jitter() ? 0.5 : 0.0)
        .atMost(retryLimit())
        .onFailure().invoke(t -> {
            LOG.infof(
                "Step %s completed all retries (%s attempts) with failure: %s",
                this.getClass().getSimpleName(),
                retryLimit(),
                t.getMessage()
            );
        });
    }
}