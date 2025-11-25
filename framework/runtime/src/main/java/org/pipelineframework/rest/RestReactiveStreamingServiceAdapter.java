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
import org.pipelineframework.service.ReactiveStreamingService;

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

    /**
     * Default constructor for RestReactiveStreamingServiceAdapter.
     */
    public RestReactiveStreamingServiceAdapter() {
    }
    

    /**
 * Provide the reactive streaming service used to process domain inputs into domain outputs.
 *
 * @return the {@link ReactiveStreamingService} instance that processes {@code DomainIn} into {@code DomainOut}
 */
protected abstract ReactiveStreamingService<DomainIn, DomainOut> getService();

    /**
 * Convert a processed domain object to its corresponding DTO representation.
 *
 * @param domainOut the domain-level result to convert
 * @return the DTO representation of the provided domain object
 */
protected abstract DtoOut toDto(DomainOut domainOut);

    /**
 * Indicates whether entities are persisted automatically before processing.
 *
 * @return true if entities are persisted automatically before processing, false otherwise
 */
    protected abstract boolean isAutoPersistenceEnabled();

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