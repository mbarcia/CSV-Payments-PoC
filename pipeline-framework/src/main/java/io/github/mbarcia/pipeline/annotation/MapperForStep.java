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
 * Annotation to mark a mapper class that handles conversions between different representations
 * of the same entity: domain, DTO, and gRPC formats.
 * This annotation enables automatic generation of gRPC and REST adapters.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MapperForStep {
    /**
     * The order of the step this mapper is for.
     * Should match the order of the corresponding @PipelineStep.
     * @return the order of the step
     */
    int order() default 0;
    
    /**
     * The gRPC message class for this entity.
     * @return the gRPC message class
     */
    Class<?> grpc() default Void.class;
    
    /**
     * The DTO class for this entity.
     * @return the DTO class
     */
    Class<?> dto() default Void.class;
    
    /**
     * The domain class for this entity.
     * @return the domain class
     */
    Class<?> domain() default Void.class;
    
    /**
     * The domain input type for this mapper.
     * Used for automatic adapter generation to map from gRPC to domain.
     * @return the domain input type class
     */
    Class<?> domainInputType() default Void.class;
    
    /**
     * The domain output type for this mapper.
     * Used for automatic adapter generation to map from domain to gRPC.
     * @return the domain output type class
     */
    Class<?> domainOutputType() default Void.class;
}