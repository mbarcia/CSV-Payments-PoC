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

package org.pipelineframework.grpc;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveStreamingService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GrpcServiceStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  private static final Logger LOG = LoggerFactory.getLogger(GrpcServiceStreamingAdapter.class);

  @Inject
  PersistenceManager persistenceManager;
  
  /**
   * Sets the persistence manager for this adapter.
   * This method is useful when the adapter is not managed by CDI (e.g., anonymous inner classes).
   * 
   * @param persistenceManager the persistence manager to use
   */
  public void setPersistenceManager(PersistenceManager persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  protected abstract ReactiveStreamingService<DomainIn, DomainOut> getService();

  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  protected abstract GrpcOut toGrpc(DomainOut domainOut);

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

  public Multi<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
    DomainIn entity = fromGrpc(grpcRequest);
    
    Uni<DomainIn> persistenceUni;
      if (isAutoPersistenceEnabled()) {
        LOG.debug("Auto-persistance is enabled");
        persistenceUni = persistenceManager.persist(entity);
      }
      else {
        LOG.debug("Auto-persistance is disabled");
        persistenceUni = Uni.createFrom().item(entity);
      }

      return persistenceUni
        .onItem().transformToMulti(persistedEntity -> 
            getService()
                .process(persistedEntity) // Multi<DomainOut>
                .onItem()
                .transform(this::toGrpc) // Multi<GrpcOut>
                .onFailure()
                .transform(new throwStatusRuntimeExceptionFunction())
        );
  }
}
