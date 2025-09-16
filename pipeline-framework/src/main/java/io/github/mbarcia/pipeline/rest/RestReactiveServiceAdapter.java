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

import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

/**
 * Base class for REST resources that provides auto-persistence functionality.
 * 
 * @param <DomainIn> The domain input type
 * @param <DomainOut> The domain output type
 * @param <DtoOut> The DTO output type
 */
public abstract class RestReactiveServiceAdapter<DomainIn, DomainOut, DtoOut> {

    @Inject
    PersistenceManager persistenceManager;

    protected abstract ReactiveService<DomainIn, DomainOut> getService();

    protected abstract DtoOut toDto(DomainOut domainOut);

    /**
     * Get the step configuration for this service adapter.
     * Override this method to provide specific configuration.
     * 
     * @return the step configuration, or null if not configured
     */
    protected StepConfig getStepConfig() {
        return null;
    }

    /**
     * Determines whether entities should be automatically persisted before processing.
     * Override this method to enable auto-persistence.
     * 
     * @return true if entities should be auto-persisted, false otherwise
     */
    protected boolean isAutoPersistenceEnabled() {
        StepConfig config = getStepConfig();
        return config != null && config.autoPersist();
    }

    /**
     * Process a single domain object with auto-persistence support.
     * 
     * @param domainObject The domain object to process
     * @return A Uni that completes with the DTO result
     */
    protected Uni<DtoOut> processWithAutoPersistence(DomainIn domainObject) {
        Uni<DomainIn> persistenceUni = isAutoPersistenceEnabled() 
            ? persistenceManager.persist(domainObject)
            : Uni.createFrom().item(domainObject);
        
        return persistenceUni
            .onItem().transformToUni(persistedEntity -> 
                getService()
                    .process(persistedEntity)
                    .onItem()
                    .transform(this::toDto)
            );
    }

    /**
     * Process a stream of domain objects with auto-persistence support.
     * 
     * @param domainStream The stream of domain objects to process
     * @return A Multi that emits DTO results
     */
    protected Multi<DtoOut> processStreamWithAutoPersistence(Multi<DomainIn> domainStream) {
        Multi<DomainIn> persistedStream = isAutoPersistenceEnabled() 
            ? domainStream.onItem().transformToUniAndMerge(persistenceManager::persist)
            : domainStream;

        return persistedStream
            .onItem().transformToUniAndMerge(entity -> 
                getService()
                    .process(entity)
                    .onItem()
                    .transform(this::toDto)
            );
    }
}