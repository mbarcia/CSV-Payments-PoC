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

package io.github.mbarcia.pipeline.grpc;

import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.github.mbarcia.pipeline.service.ReactiveStreamingService;
import io.github.mbarcia.pipeline.service.throwStatusRuntimeExceptionFunction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

public abstract class GrpcServiceStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  @Inject
  PersistenceManager persistenceManager;

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
    
    Uni<DomainIn> persistenceUni = isAutoPersistenceEnabled() 
        ? persistenceManager.persist(entity)
        : Uni.createFrom().item(entity);
    
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
