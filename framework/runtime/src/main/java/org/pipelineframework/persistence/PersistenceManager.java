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

package org.pipelineframework.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Manager for persistence operations that delegates to registered PersistenceProvider implementations.
 */
@ApplicationScoped
public class PersistenceManager {

    private static final Logger LOG = Logger.getLogger(PersistenceManager.class);

    private List<PersistenceProvider<?>> providers;

    @Inject
    Instance<PersistenceProvider<?>> providerInstance;

    @PostConstruct
    void init() {
        this.providers = providerInstance.stream().toList();
        LOG.infof("Initialised %s persistence providers", providers.size());
    }

    /**
     * Persist an entity using the appropriate provider.
     *
     * @param entity The entity to persist
     * @return A Uni that completes with the persisted entity, or the original entity if no provider supports it
     */
    public <T> Uni<T> persist(T entity) {
        if (entity == null) {
            LOG.debug("Entity is null, returning empty Uni");
            return Uni.createFrom().nullItem();
        }

        LOG.debugf("Entity to persist: %s", entity.getClass().getName());
        for (PersistenceProvider<?> provider : providers) {
            if (provider.supports(entity)) {
                @SuppressWarnings("unchecked")
                PersistenceProvider<T> p = (PersistenceProvider<T>) provider;
                LOG.debugf("About to persist with provider: %s", provider.getClass().getName());

                return p.persist(entity);
            }
        }

        LOG.warnf("No persistence provider found for %s", entity.getClass().getName());
        return Uni.createFrom().item(entity);
    }
}
