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

package io.github.mbarcia.pipeline.processor;

import io.github.mbarcia.pipeline.StepsRegistry;
import io.quarkus.runtime.annotations.Recorder;
import java.util.function.Supplier;

@Recorder
public class PipelineRecorder {

    public Supplier<StepsRegistry> createStepsRegistry(String className) {
        return () -> {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Class<?> clazz = classLoader.loadClass(className);
                return (StepsRegistry) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create StepsRegistry instance", e);
            }
        };
    }
}