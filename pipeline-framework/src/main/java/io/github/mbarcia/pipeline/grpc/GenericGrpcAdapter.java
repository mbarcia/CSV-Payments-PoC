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

package io.github.mbarcia.pipeline.grpc;

import io.github.mbarcia.pipeline.adapter.StepConfigProvider;
import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericGrpcAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> 
        extends GrpcReactiveServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericGrpcAdapter.class);

    @Inject
    PersistenceManager persistenceManager;
    
    // The step class this adapter is for
    private Class<? extends ConfigurableStep> stepClass;
    
    // The mapper for this adapter
    private Object mapper;
    
    // The service for this adapter
    private ReactiveService<DomainIn, DomainOut> service;
    
    // Input and output types
    private Class<DomainIn> domainInType;
    private Class<DomainOut> domainOutType;
    
    /**
     * Sets the persistence manager for this adapter.
     * This method is useful when the adapter is not managed by CDI (e.g., anonymous inner classes).
     * 
     * @param persistenceManager the persistence manager to use
     */
    public void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }
    
    /**
     * Sets the step class this adapter is for.
     * 
     * @param stepClass the step class
     */
    public void setStepClass(Class<? extends ConfigurableStep> stepClass) {
        this.stepClass = stepClass;
    }
    
    /**
     * Sets the mapper for this adapter.
     * 
     * @param mapper the mapper
     */
    public void setMapper(Object mapper) {
        this.mapper = mapper;
    }
    
    /**
     * Sets the service for this adapter.
     * 
     * @param service the service
     */
    public void setService(ReactiveService<DomainIn, DomainOut> service) {
        this.service = service;
    }
    
    /**
     * Sets the domain input type.
     * 
     * @param domainInType the domain input type
     */
    public void setDomainInType(Class<DomainIn> domainInType) {
        this.domainInType = domainInType;
    }
    
    /**
     * Sets the domain output type.
     * 
     * @param domainOutType the domain output type
     */
    public void setDomainOutType(Class<DomainOut> domainOutType) {
        this.domainOutType = domainOutType;
    }

    @Override
    protected ReactiveService<DomainIn, DomainOut> getService() {
        return service;
    }

    @Override
    protected DomainIn fromGrpc(GrpcIn grpcIn) {
        try {
            // Use reflection to call the mapper's fromGrpc method
            return (DomainIn) mapper.getClass()
                .getMethod("fromGrpc", grpcIn.getClass())
                .invoke(mapper, grpcIn);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map from gRPC to domain", e);
        }
    }

    @Override
    protected GrpcOut toGrpc(DomainOut domainOut) {
        try {
            // Use reflection to call the mapper's toGrpc method
            return (GrpcOut) mapper.getClass()
                .getMethod("toGrpc", domainOut.getClass())
                .invoke(mapper, domainOut);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map from domain to gRPC", e);
        }
    }

    /**
     * Get the step configuration for this service adapter.
     * Override this method to provide specific configuration.
     * 
     * @return the step configuration, or null if not configured
     */
    @Override
    protected StepConfig getStepConfig() {
        if (stepClass != null) {
            return StepConfigProvider.getStepConfig(stepClass);
        }
        return null;
    }

    /**
     * Determines whether entities should be automatically persisted before processing.
     * Override this method to enable auto-persistence.
     * 
     * @return true if entities should be auto-persisted, false otherwise
     */
    @Override
    protected boolean isAutoPersistenceEnabled() {
        if (stepClass != null) {
            return StepConfigProvider.isAutoPersistenceEnabled(stepClass);
        }
        
        StepConfig config = getStepConfig();
        return config != null && config.autoPersist();
    }
}