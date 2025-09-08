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

package io.github.mbarcia.csv.service;

import io.smallrye.mutiny.Multi;

/**
 * Step supplier that takes a single input and produces a stream of outputs.
 * This is useful for processing steps that expand one input into multiple outputs.
 * 
 * @param <IN> Input type
 * @param <OUT> Output type for the stream
 */
public interface UniToMultiStep<IN, OUT> extends PipelineStep<IN, Multi<OUT>> {
    // Inherits execute method from PipelineStep
}