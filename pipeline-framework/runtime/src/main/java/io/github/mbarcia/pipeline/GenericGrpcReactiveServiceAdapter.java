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

package io.github.mbarcia.pipeline;

import io.github.mbarcia.pipeline.annotation.StepConfigProvider;
import io.github.mbarcia.pipeline.mapper.InboundMapper;
import io.github.mbarcia.pipeline.mapper.OutboundMapper;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.github.mbarcia.pipeline.service.throwStatusRuntimeExceptionFunction;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
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
public class GenericGrpcReactiveServiceAdapter<GRpcIn, DomainIn, DomainOut, GRpcOut> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericGrpcReactiveServiceAdapter.class);

    private final InboundMapper<GRpcIn, DomainIn> inboundMapper;
    private final OutboundMapper<DomainOut, GRpcOut> outboundMapper;
    private final ReactiveService<DomainIn, DomainOut> service;
    private final PersistenceManager persistenceManager;
    private final Class<? extends ConfigurableStep> stepClass; // The step class this adapter is for

    /**
     * Constructs a GenericGrpcReactiveServiceAdapter with the specified mappers and services.
     *
     * @param inboundMapper  the inbound mapper
     * @param outboundMapper the outbound mapper
     * @param service        the service
     */
    public GenericGrpcReactiveServiceAdapter(InboundMapper<GRpcIn, DomainIn> inboundMapper,
                                             OutboundMapper<DomainOut, GRpcOut> outboundMapper,
                                             ReactiveService<DomainIn, DomainOut> service,
                                             PersistenceManager persistenceManager, 
                                             Class<? extends ConfigurableStep> stepClass) {
        this.inboundMapper = inboundMapper;
        this.outboundMapper = outboundMapper;
        this.service = service;
        this.persistenceManager = persistenceManager;
        this.stepClass = stepClass;
    }

    /**
     * Determines whether entities should be automatically persisted before processing.
     * Override this method to enable auto-persistence.
     *
     * @return true if entities should be auto-persisted, false otherwise
     */
    protected boolean isAutoPersistenceEnabled() {
        return StepConfigProvider.isAutoPersistenceEnabled(stepClass);
    }

    public Uni<GRpcOut> remoteProcess(GRpcIn grpcRequest) {
        DomainIn entity = inboundMapper.toDomain(grpcRequest);

        Uni<DomainIn> persistenceUni;
        if (isAutoPersistenceEnabled()) {
            LOG.debug("Auto-persistance is enabled");
            persistenceUni = persistenceManager.persist(entity);
        }
        else {
            LOG.debug("Auto-persistance is disabled");
            persistenceUni = Uni.createFrom().item(entity);
        }

        return persistenceUni
            .onItem().transformToUni(persistedEntity -> service
                .process(persistedEntity)
                .onItem()
                .transform(outboundMapper::toGrpc)
                .onFailure()
                .transform(new throwStatusRuntimeExceptionFunction())
            );
    }
}