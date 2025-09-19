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

public interface StepManyToMany extends Step {
    Multi<Object> applyStreaming(Multi<Object> upstream);

    default Multi<?> deadLetterMulti(Multi<Object> upstream, Throwable err) {
        System.err.print("DLQ drop");
        return Multi.createFrom().empty();
    }
    
    @Override
    default Multi<Object> apply(Multi<Object> input) {
        final java.util.concurrent.Executor vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        java.util.concurrent.Executor executor = runWithVirtualThreads() ? vThreadExecutor : null;

        return Multi.createFrom().deferred(() -> {
            Multi<Object> baseUpstream = (executor != null)
                    ? input.runSubscriptionOn(executor)
                    : input;

            Multi<Object> out = applyStreaming(baseUpstream);

            return out
                .onFailure().retry()
                .withBackOff(retryWait(), maxBackoff())
                .withJitter(jitter() ? 0.5 : 0.0)
                .atMost(retryLimit())
                .onFailure().recoverWithMulti(err -> {
                    if (recoverOnFailure()) {
                        if (debug()) {
                            System.err.printf("Step %s failed streaming: %s%n",
                                    this.getClass().getSimpleName(), err);
                        }
                        return deadLetterMulti(baseUpstream, err); // custom DLQ handling
                    } else {
                        return Multi.createFrom().failure(err);
                    }
                })
                .onItem().invoke(o -> {
                    if (debug()) {
                        System.out.printf("Step %s streamed item: %s%n",
                                this.getClass().getSimpleName(), o);
                    }
                })
                .onCompletion().invoke(() -> {
                    if (debug()) {
                        System.out.printf("Step %s completed streaming%n",
                                this.getClass().getSimpleName());
                    }
                });
        });
    }
}
