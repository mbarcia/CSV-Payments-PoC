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

package io.github.mbarcia.pipeline.rest;

import io.github.mbarcia.pipeline.adapter.StepConfigProvider;
import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.github.mbarcia.pipeline.service.ReactiveStreamingService;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

/**
 * Base class for streaming REST resources that provides auto-persistence functionality.
 * 
 * @param <DomainIn> The domain input type
 * @param <DomainOut> The domain output type
 * @param <DtoOut> The DTO output type
 */
public abstract class RestReactiveStreamingServiceAdapter<DomainIn, DomainOut, DtoOut> {

    @Inject
    PersistenceManager persistenceManager;
    
    // The step class this adapter is for
    private Class<? extends ConfigurableStep> stepClass;

    protected abstract ReactiveStreamingService<DomainIn, DomainOut> getService();

    protected abstract DtoOut toDto(DomainOut domainOut);
    
    /**
     * Sets the step class this adapter is for.
     * 
     * @param stepClass the step class
     */
    public void setStepClass(Class<? extends ConfigurableStep> stepClass) {
        this.stepClass = stepClass;
    }

    /**
     * Get the step configuration for this service adapter.
     * Override this method to provide specific configuration.
     * 
     * @return the step configuration, or null if not configured
     */
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
    protected boolean isAutoPersistenceEnabled() {
        if (stepClass != null) {
            return StepConfigProvider.isAutoPersistenceEnabled(stepClass);
        }
        
        StepConfig config = getStepConfig();
        return config != null && config.autoPersist();
    }

    /**
     * Process a single domain object with auto-persistence support.
     * 
     * @param domainObject The domain object to process
     * @return A Multi that emits DTO results
     */
    protected Multi<DtoOut> processWithAutoPersistence(DomainIn domainObject) {
        Uni<DomainIn> persistenceUni = isAutoPersistenceEnabled() 
            ? persistenceManager.persist(domainObject)
            : Uni.createFrom().item(domainObject);
        
        return persistenceUni
            .onItem().transformToMulti(persistedEntity -> 
                getService()
                    .process(persistedEntity)
                    .onItem()
                    .transform(this::toDto)
            );
    }
}