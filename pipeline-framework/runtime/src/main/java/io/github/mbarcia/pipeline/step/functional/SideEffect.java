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

package io.github.mbarcia.pipeline.step.functional;

import io.smallrye.mutiny.Uni;

/**
 * Functional interface for side-effect step transformations.
 * 
 * @param <I> Input type
 */
@FunctionalInterface
public interface SideEffect<I> {
    /**
     * Apply a side effect to a single input item.
     * 
     * @param input The input item
     * @return A Uni that completes when the side effect is done
     */
    Uni<Void> apply(I input);
}