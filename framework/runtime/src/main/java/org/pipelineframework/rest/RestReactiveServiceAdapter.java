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

package org.pipelineframework.rest;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveService;

/**
 * Base class for REST resources that provides auto-persistence functionality.
 * 
 * @param <DomainIn> The domain input type
 * @param <DomainOut> The domain output type
 * @param <DtoOut> The DTO output type
 */
public abstract class RestReactiveServiceAdapter<DomainIn, DomainOut, DtoOut> {

    /**
     * Default constructor for RestReactiveServiceAdapter.
     */
    @Inject
    PersistenceManager persistenceManager;

    /**
 * Provides the reactive service used to process domain inputs into domain outputs.
 *
 * @return the {@code ReactiveService<DomainIn, DomainOut>} instance used to process domain objects
 */
protected abstract ReactiveService<DomainIn, DomainOut> getService();

    /**
 * Convert a processed domain object to its REST DTO representation.
 *
 * @param domainOut the processed domain model instance to convert
 * @return the DTO representation to be returned by the REST resource
 */
protected abstract DtoOut toDto(DomainOut domainOut);

    /**
 * Indicate whether entities must be persisted automatically before processing.
 *
 * Implementations (typically generated service adapters) should return the auto-persist
 * setting derived from the @PipelineStep annotation.
 *
 * @return `true` if entities must be auto-persisted before processing, `false` otherwise
 */
    protected abstract boolean isAutoPersistenceEnabled();

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
            ? domainStream.onItem().transformToUniAndConcatenate(persistenceManager::persist)
            : domainStream;

        return persistedStream
            .onItem().transformToUniAndConcatenate(entity ->
                getService()
                    .process(entity)
                    .onItem()
                    .transform(this::toDto)
            );
    }
}