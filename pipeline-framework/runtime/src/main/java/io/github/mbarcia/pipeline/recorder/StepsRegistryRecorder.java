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

package io.github.mbarcia.pipeline.recorder;

import io.github.mbarcia.pipeline.StepsRegistry;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import java.text.MessageFormat;

@Recorder
public class StepsRegistryRecorder {

    public RuntimeValue<StepsRegistry> createStepsRegistry(String className) {
        // Create and return a RuntimeValue containing a StepsRegistry implementation
        // The actual instantiation will happen at runtime in the application
        return new RuntimeValue<>(new LazyStepsRegistry(className));
    }

    // Inner class to handle lazy instantiation
    private static class LazyStepsRegistry implements StepsRegistry {
        private final String className;
        private volatile Object instance;  // Keep as Object to avoid cast issues

        public LazyStepsRegistry(String className) {
            this.className = className;
        }

        @Override
        public java.util.List<Object> getSteps() {
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) {
                        try {
                            ClassLoader cl = StepsRegistry.class.getClassLoader();
                            Class<?> clazz = Class.forName(className, true, cl);
                            Object obj = clazz.getDeclaredConstructor().newInstance();
                            
                            // Verify that the generated class has the required getSteps method
                            clazz.getMethod("getSteps");
                            
                            instance = obj;
                        } catch (Exception e) {
                            throw new RuntimeException(MessageFormat.format("Failed to instantiate StepsRegistryImpl: {0}", className), e);
                        }
                    }
                }
            }
            
            try {
                // Use reflection to call getSteps on the instance
                java.lang.reflect.Method getStepsMethod = instance.getClass().getMethod("getSteps");
                return (java.util.List<Object>) getStepsMethod.invoke(instance);
            } catch (Exception e) {
                throw new RuntimeException("Failed to call getSteps method on StepsRegistryImpl", e);
            }
        }
    }
}