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
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;
import org.pipelineframework.step.functional.OneToMany;

/** 1 -> N */
public interface StepOneToMany<I, O> extends OneToMany<I, O>, Configurable, DeadLetterQueue<I, O> {
    Multi<O> applyOneToMany(I in);

	@Override
    default Multi<O> apply(Uni<I> input) {
        final Logger LOG = Logger.getLogger(this.getClass());

        return input.onItem().transformToMulti(item -> {
            Multi<O> multi = applyOneToMany(item);

            // Apply overflow strategy
            if ("buffer".equalsIgnoreCase(backpressureStrategy())) {
                multi = multi.onOverflow().buffer(backpressureBufferCapacity());
            } else if ("drop".equalsIgnoreCase(backpressureStrategy())) {
                multi = multi.onOverflow().drop();
            } else {
                // default behavior - buffer with default capacity
                multi = multi.onOverflow().buffer(128); // default buffer size
            }

            return multi.onItem().transform(o -> {
                LOG.debugf(
                    "Step %s emitted item: %s",
                    this.getClass().getSimpleName(), o
                );
                return o;
            });
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