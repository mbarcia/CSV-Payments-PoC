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

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;
import org.pipelineframework.persistence.PersistenceProvider;

@Dependent
public class VThreadPersistenceProvider implements PersistenceProvider<Object> {

  private static final Logger LOG = Logger.getLogger(VThreadPersistenceProvider.class);

  private final InjectableInstance<EntityManager> entityManagerInstance;

  public VThreadPersistenceProvider() {
    // Look up the EntityManager bean instance via Arc
    entityManagerInstance = Arc.container().select(EntityManager.class);
  }

  @Override
  public Uni<Object> persist(Object entity) {
    return Uni.createFrom().item(() -> {
      if (!entityManagerInstance.isResolvable()) {
        throw new IllegalStateException("No EntityManager available for VThreadPersistenceProvider");
      }

	    try (EntityManager em = entityManagerInstance.get()) {
		    em.getTransaction().begin();
		    try {
			    em.persist(entity);
			    em.getTransaction().commit();
		    } catch (Exception e) {
			    if (em.getTransaction().isActive()) {
				    em.getTransaction().rollback();
			    }
			    throw e;
		    }
		    return entity;
	    }
    });
  }

  @Override
  public Class<Object> type() {
    return Object.class;
  }

  @Override
  public boolean supports(Object entity) {
    return entity.getClass().isAnnotationPresent(Entity.class);
  }
}
