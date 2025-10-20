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

package org.pipelineframework.persistence.provider;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.pipelineframework.persistence.PersistenceProvider;
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
    public Uni<PanacheEntityBase> persist(PanacheEntityBase entity) {
        if (entity == null) {
            LOG.debug("Null entity received and returned");
            return Uni.createFrom().nullItem();
        }

        if (entity instanceof PanacheEntityBase panacheEntity) {
            LOG.debug("About to persist entity: {}", entity);

            return Panache.withTransaction(() ->
                            panacheEntity.persistAndFlush()
                                    .replaceWith(panacheEntity)
                    )
                    .onFailure().recoverWithUni(failure -> {
                        LOG.debug("Error during persist: {}", failure.getMessage());

                        if (isDuplicateKeyError(failure)) {
                            LOG.debug("Duplicate key detected, ignoring persist and returning entity");
                            // Retry outside of transaction, return entity safely
                            return Uni.createFrom().item(panacheEntity);
                        }

                        LOG.error("Unexpected persistence failure for {}: {}",
                                entity.getClass().getSimpleName(), failure.getMessage(), failure);
                        return Uni.createFrom().failure(failure);
                    });
        } else {
            LOG.debug("Skipped non-Panache entity");
        }

        return Uni.createFrom().item(entity);
    }
    
    /**
     * Checks if the failure is a duplicate key constraint violation
     */
    private boolean isDuplicateKeyError(Throwable failure) {
        String message = failure.getMessage();
        if (message != null) {
            return message.contains("duplicate key value violates unique constraint") ||
                   message.contains("Unique index or primary key violation") ||
                   message.contains("Duplicate entry");
        }
        return false;
    }

    @Override
    public boolean supports(Object entity) {
        return entity instanceof PanacheEntityBase;
    }
}