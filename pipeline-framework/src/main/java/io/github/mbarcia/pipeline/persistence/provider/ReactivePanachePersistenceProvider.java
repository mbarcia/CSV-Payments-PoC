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

package io.github.mbarcia.pipeline.persistence.provider;

import io.github.mbarcia.pipeline.persistence.PersistenceProvider;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * Reactive persistence provider using Hibernate Reactive Panache.
 */
@ApplicationScoped
public class ReactivePanachePersistenceProvider implements PersistenceProvider<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(ReactivePanachePersistenceProvider.class);

    @Override
    public Uni<Object> persist(Object entity) {
        if (entity instanceof PanacheEntityBase panacheEntity) {
            LOG.debug(MessageFormat.format("About to persist entity: {0}", entity));

            return panacheEntity.persistAndFlush()
                .onItem().transform(_ -> entity)
                .onFailure().recoverWithUni(t -> {
                    // Log the error but don't fail the operation
                    LOG.error(MessageFormat.format("Failed to persist entity with Hibernate Reactive: {0}", t.getMessage()));
                    return Uni.createFrom().item(entity);
                });
        } else {
            LOG.debug(MessageFormat.format("Skipped entity: {0}", entity));
        }

        return Uni.createFrom().item(entity);
    }

    @Override
    public boolean supports(Object entity) {
        return entity instanceof PanacheEntityBase;
    }
}