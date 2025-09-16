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

package io.github.mbarcia.pipeline.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manager for persistence operations that delegates to registered PersistenceProvider implementations.
 */
@ApplicationScoped
public class PersistenceManager {

    @Inject
    Instance<PersistenceProvider<?>> providers;

    /**
     * Persist an entity using the appropriate provider.
     * 
     * @param entity The entity to persist
     * @return A Uni that completes with the persisted entity, or the original entity if no provider supports it
     */
    public <T> Uni<T> persist(T entity) {
        if (entity == null) {
            return Uni.createFrom().item(entity);
        }

        // Find a provider that supports this entity
        PersistenceProvider<T> provider = findProvider(entity);
        
        if (provider != null) {
            return provider.persist(entity);
        }
        
        // No provider found, return the entity as-is
        return Uni.createFrom().item(entity);
    }

    /**
     * Find a provider that supports the given entity.
     * 
     * @param entity The entity to find a provider for
     * @return A provider that supports the entity, or null if none found
     */
    @SuppressWarnings("unchecked")
    private <T> PersistenceProvider<T> findProvider(T entity) {
        if (providers.isUnsatisfied()) {
            return null;
        }

        List<PersistenceProvider<?>> providerList = providers.stream().collect(Collectors.toList());
        
        for (PersistenceProvider<?> provider : providerList) {
            if (provider.supports(entity)) {
                return (PersistenceProvider<T>) provider;
            }
        }
        
        return null;
    }
}