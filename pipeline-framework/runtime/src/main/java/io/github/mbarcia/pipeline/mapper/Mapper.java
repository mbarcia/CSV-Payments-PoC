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

package io.github.mbarcia.pipeline.mapper;

/**
 * Interface for mapping from/to gRPC DTOs and domain.
 *
 * @param <Grpc>  the gRPC type
 * @param <Dto>   the DTO type
 * @param <Domain> the domain type
 */
public interface Mapper<Grpc, Dto, Domain> {
    /**
     * Converts from gRPC input type to domain input type.
     *
     * @param grpcIn the gRPC input object
     * @return the domain input object
     */
    default Domain fromGrpcFromDto(Grpc grpcIn) {
        return fromDto(fromGrpc(grpcIn));
    }

    default Grpc toDtoToGrpc(Domain domain) {
        return toGrpc(toDto(domain));
    }

    Dto fromGrpc(Grpc grpc);
    Grpc toGrpc(Dto dto);

    Domain fromDto(Dto dto);
    Dto toDto(Domain domain);
}