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

package io.github.mbarcia.pipeline.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for persistence operations that delegates to registered PersistenceProvider implementations.
 */
@ApplicationScoped
public class PersistenceManager {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceManager.class);

    private List<PersistenceProvider<?>> providers;

    @Inject
    Instance<PersistenceProvider<?>> providerInstance;

    @PostConstruct
    void init() {
        this.providers = providerInstance.stream().toList();
        LOG.info("Initialized {} persistence providers", providers.size());
    }

    /**
     * Persist an entity using the appropriate provider.
     *
     * @param entity The entity to persist
     * @return A Uni that completes with the persisted entity, or the original entity if no provider supports it
     */
    public <T> Uni<T> persist(T entity) {
        if (entity == null) {
            return Uni.createFrom().item((T) null);
        }

        for (PersistenceProvider<?> provider : providers) {
            if (provider.supports(entity)) {
                @SuppressWarnings("unchecked")
                PersistenceProvider<T> p = (PersistenceProvider<T>) provider;
                return p.persist(entity);
            }
        }

        LOG.warn("No persistence provider found for {}", entity.getClass().getName());
        return Uni.createFrom().item(entity);
    }
}
