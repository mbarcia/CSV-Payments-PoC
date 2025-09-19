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

package io.github.mbarcia.pipeline.step;

import io.smallrye.mutiny.Multi;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 1 -> N */
public interface StepOneToMany<I, O> extends Step {
    Multi<O> applyMulti(I in);
    
    @Override
    default Multi<Object> apply(Multi<Object> input) {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        java.util.concurrent.Executor executor = runWithVirtualThreads() ? vThreadExecutor : null;
        int concurrency = Math.max(1, effectiveConfig().concurrency());

        return input.onItem().transformToMulti(item -> {
            Multi<O> typedMulti = applyMulti((I) item);
            Multi<Object> multi = typedMulti.onItem().transform(o -> (Object) o);

            if (executor != null) {
                // shift blocking subscription work to virtual threads
                multi = multi.runSubscriptionOn(executor);
            }

            return multi.onItem().transform(o -> {
                if (debug()) {
                    LOG.debug(
                        "Step {0} emitted item: {}{}",
                        this.getClass().getSimpleName(), o
                    );
                }
                return o;
            });
        }).merge(concurrency);
    }
}
