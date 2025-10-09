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

package io.github.mbarcia.pipeline.persistence.provider;

import io.github.mbarcia.pipeline.persistence.PersistenceProvider;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reactive persistence provider using Hibernate Reactive Panache.
 * This provider only activates when Hibernate Reactive classes are available,
 * which happens required dependencies are present.
 */
@ApplicationScoped
public class ReactivePanachePersistenceProvider implements PersistenceProvider<PanacheEntityBase> {

    private static final Logger LOG = LoggerFactory.getLogger(ReactivePanachePersistenceProvider.class);

    @Override
    public Class<PanacheEntityBase> type() {
        return PanacheEntityBase.class;
    }

    @Override
    @WithTransaction
    public Uni<PanacheEntityBase> persist(PanacheEntityBase entity) {
        if (entity == null) {
            LOG.debug("Null entity received and returned");
            return Uni.createFrom().nullItem();
        }
        if (entity instanceof PanacheEntityBase panacheEntity) {
            LOG.debug("About to persist entity: {}", entity);

            // Directly persist the entity without wrapping in Panache.withSession()
            // since we're already in a transactional context
            return panacheEntity.persistAndFlush()
                .replaceWith(entity)
                .onFailure().recoverWithUni(t -> {
                    // Log the error but don't fail the operation
                    LOG.error("Failed to persist {}: {}", entity.getClass().getSimpleName(), t.getMessage(), t);
                    return Uni.createFrom().item(entity);
                });
        } else {
            LOG.debug("Skipped non-Panache entity");
        }

        return Uni.createFrom().item(entity);
    }

    @Override
    public boolean supports(Object entity) {
        return entity instanceof PanacheEntityBase;
    }
}