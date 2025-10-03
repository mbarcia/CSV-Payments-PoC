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
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.github.mbarcia.pipeline.service.throwStatusRuntimeExceptionFunction;
import io.smallrye.mutiny.Uni;
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
public abstract class GenericGrpcReactiveServiceAdapter<GRpcIn, DomainIn, DomainOut, GRpcOut> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericGrpcReactiveServiceAdapter.class);

    public abstract Mapper<GRpcIn, ?, DomainIn> getInboundMapper();
    public abstract Mapper<GRpcOut, ?, DomainOut> getOutboundMapper() ;
    public abstract ReactiveService<DomainIn, DomainOut> getService();
    public abstract PersistenceManager getPersistenceManager();

    /**
     * Determines whether entities should be automatically persisted before processing.
     * Override this method to enable auto-persistence.
     *
     * @return true if entities should be auto-persisted, false otherwise
     */
    protected abstract boolean isAutoPersistenceEnabled();

    public Uni<GRpcOut> remoteProcess(GRpcIn grpcRequest) {
        DomainIn entity = getInboundMapper().fromGrpcFromDto(grpcRequest);

        Uni<DomainIn> persistenceUni = getPersistedUni(entity);

        return persistenceUni
            .onItem().transformToUni(persistedEntity -> getService()
                .process(persistedEntity)
                .onItem()
                .transform(getOutboundMapper()::toDtoToGrpc)
                .onFailure()
                .transform(new throwStatusRuntimeExceptionFunction())
            );
    }

    private Uni<DomainIn> getPersistedUni(DomainIn entity) {
        if (isAutoPersistenceEnabled()) {
            LOG.debug("Auto-persistence is enabled");
            return getPersistenceManager().persist(entity);
        }
        else {
            LOG.debug("Auto-persistence is disabled");
            return Uni.createFrom().item(entity);
        }
    }
}