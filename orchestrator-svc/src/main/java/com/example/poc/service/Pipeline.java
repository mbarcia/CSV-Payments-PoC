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

package com.example.poc.service;

import io.smallrye.mutiny.Uni;

/**
 * Generic pipeline that processes data through a series of steps.
 * This interface defines the contract for all pipeline implementations.
 * 
 * @param <IN> Input type for the entire pipeline
 * @param <OUT> Output type for the entire pipeline
 */
public interface Pipeline<IN, OUT> {
    
    /**
     * Process the input through the pipeline.
     * 
     * @param input The input to process
     * @return Uni with the final output
     */
    Uni<OUT> process(IN input);

}