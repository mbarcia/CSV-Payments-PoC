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

package io.github.mbarcia.pipeline;

import io.github.mbarcia.pipeline.mapper.Mapper;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.github.mbarcia.pipeline.service.ReactiveStreamingService;
import io.github.mbarcia.pipeline.service.throwStatusRuntimeExceptionFunction;
import io.smallrye.mutiny.Multi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic gRPC adapter that implements BindableService and delegates to mappers and service.
 *
 * @param <GRpcIn>    the gRPC input type
 * @param <DomainIn>  the domain input type
 * @param <DomainOut> the domain output type
 * @param <GRpcOut>   the gRPC output type
 */
public abstract class GenericGrpcServiceStreamingAdapter<GRpcIn, DomainIn, DomainOut, GRpcOut> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericGrpcServiceStreamingAdapter.class);

    protected abstract Mapper<GRpcIn,?,DomainIn> getInboundMapper();
    protected abstract Mapper<GRpcOut, ?, DomainOut> getOutboundMapper();
    protected abstract ReactiveStreamingService<DomainIn, DomainOut> getService();
    protected abstract PersistenceManager getPersistenceManager();

    public Multi<GRpcOut> remoteProcess(GRpcIn grpcRequest) {
        DomainIn entity = getInboundMapper().fromGrpcFromDto(grpcRequest);

        // If auto-persistence is enabled, persist after processing starts
        if (isAutoPersistenceEnabled()) {
            LOG.debug("Auto-persistence is enabled, will persist input after processing");
            
            // Process the entity and persist the input when stream is consumed
            return getService().process(entity) // Multi<DomainOut>
                .onSubscription().call(() -> 
                    // Persist the input entity when the stream is subscribed to
                    getPersistenceManager().persist(entity)
                )
                .onItem().transform(getOutboundMapper()::toDtoToGrpc) // Multi<GrpcOut>
                .onFailure().transform(new throwStatusRuntimeExceptionFunction());
        } else {
            LOG.debug("Auto-persistence is disabled");
            
            return getService().process(entity) // Multi<DomainOut>
                .onItem().transform(getOutboundMapper()::toDtoToGrpc) // Multi<GrpcOut>
                .onFailure().transform(new throwStatusRuntimeExceptionFunction());
        }
    }

    /**
     * Determines whether entities should be automatically persisted before processing.
     * Override this method to enable auto-persistence.
     *
     * @return true if entities should be auto-persisted, false otherwise
     */
    protected abstract boolean isAutoPersistenceEnabled();
}