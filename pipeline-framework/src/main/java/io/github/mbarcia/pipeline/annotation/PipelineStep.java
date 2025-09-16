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

package io.github.mbarcia.pipeline.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a pipeline step.
 * This annotation enables automatic generation of gRPC and REST adapters.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PipelineStep {
    /**
     * The order of this step in the pipeline.
     * @return the order of the step
     */
    int order() default 0;
    
    /**
     * Whether to enable auto-persistence for this step.
     * @return true if auto-persistence should be enabled, false otherwise
     */
    boolean autoPersist() default false;
    
    /**
     * Whether to enable debug mode for this step.
     * @return true if debug mode should be enabled, false otherwise
     */
    boolean debug() default false;
    
    /**
     * Whether to enable failure recovery for this step.
     * @return true if failure recovery should be enabled, false otherwise
     */
    boolean recoverOnFailure() default false;
    
    /**
     * The input type for this pipeline step.
     * @return the input type class
     */
    Class<?> inputType() default Object.class;
    
    /**
     * The output type for this pipeline step.
     * @return the output type class
     */
    Class<?> outputType() default Object.class;
}