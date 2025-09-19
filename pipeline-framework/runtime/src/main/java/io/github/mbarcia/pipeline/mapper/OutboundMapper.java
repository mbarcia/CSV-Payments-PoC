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

package io.github.mbarcia.pipeline.mapper;

/**
 * Interface for mapping from domain output type to gRPC output type.
 *
 * @param <DomainOut> the domain output type
 * @param <GRpcOut>   the gRPC output type
 */
public interface OutboundMapper<DomainOut, GRpcOut> {
    /**
     * Converts from domain output type to gRPC output type.
     *
     * @param domainOut the domain output object
     * @return the gRPC output object
     */
    GRpcOut toGrpc(DomainOut domainOut);
}