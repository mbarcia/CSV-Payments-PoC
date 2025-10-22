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

package org.pipelineframework.step.blocking;

import io.smallrye.mutiny.Uni;
import org.pipelineframework.step.StepOneToOne;

/**
 * Blocking variant of StepOneToOne that works with synchronous operations.
 * <p>
 * This interface is designed for developers who prefer blocking programming
 * and want to work with standard Java synchronous operations instead of reactive streams.
 * <p>
 * The PipelineRunner will automatically handle the conversion between reactive
 * and blocking representations, executing blocking operations on virtual threads
 * to prevent platform thread blocking.
 */
public interface StepOneToOneBlocking<I, O> extends StepOneToOne<I, O> {
    Uni<O> apply(I in);

	default boolean runWithVirtualThreads() { return true; }
}