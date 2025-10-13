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

package org.pipelineframework.step.functional;

import io.smallrye.mutiny.Uni;

/**
 * Functional interface for 1:1 (one-to-one) step transformations.
 * 
 * @param <I> Input type
 * @param <O> Output type
 */
@FunctionalInterface
public interface OneToOne<I, O> {
    /**
     * Transform a single input item into a single output item.
     * 
     * @param input The input item
     * @return A Uni that completes with the transformed output item
     */
    Uni<O> apply(Uni<I> input);
}