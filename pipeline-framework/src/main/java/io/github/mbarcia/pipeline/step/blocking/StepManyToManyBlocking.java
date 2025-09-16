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

package io.github.mbarcia.pipeline.step.blocking;

import io.github.mbarcia.pipeline.step.StepBase;
import java.util.Collections;
import java.util.List;

/**
 * Imperative variant of StepManyToMany that works with Lists instead of Multi.
 * 
 * This interface is designed for developers who prefer imperative programming
 * and want to work with standard Java collections instead of reactive streams.
 * 
 * The PipelineRunner will automatically handle the conversion between reactive
 * and imperative representations.
 */
public interface StepManyToManyBlocking extends StepBase {
    List<Object> applyStreamingList(List<Object> upstream);

    default List<Object> deadLetterList(List<Object> upstream, Throwable err) {
        System.err.print("DLQ drop");
        return Collections.emptyList();
    }

    default boolean runWithVirtualThreads() { return false; }
}