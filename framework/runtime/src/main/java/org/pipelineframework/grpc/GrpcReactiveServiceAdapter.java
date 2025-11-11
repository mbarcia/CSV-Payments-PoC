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

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.pipelineframework.annotation.StepConfigProvider;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;
import org.pipelineframework.step.ConfigurableStep;
import org.jboss.logging.Logger;

@SuppressWarnings("LombokSetterMayBeUsed")
public abstract class GrpcReactiveServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  private static final Logger LOG = Logger.getLogger(GrpcReactiveServiceAdapter.class);

  @Inject
  PersistenceManager persistenceManager;
  
  // The step class this adapter is for
  private Class<? extends ConfigurableStep> stepClass;
  
  /**
   * Sets the persistence manager for this adapter.
   * This method is useful when the adapter is not managed by CDI (e.g., anonymous inner classes).
   * 
   * @param persistenceManager the persistence manager to use
   */
  public void setPersistenceManager(PersistenceManager persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  /**
   * Sets the step class this adapter is for.
   *
   * @param stepClass the step class
   */
  public void setStepClass(Class<? extends ConfigurableStep> stepClass) {
    this.stepClass = stepClass;
  }

  protected abstract ReactiveService<DomainIn, DomainOut> getService();

  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  protected abstract GrpcOut toGrpc(DomainOut domainOut);

  /**
   * Get the step configuration for this service adapter.
   * Override this method to provide specific configuration.
   * 
   * @return the step configuration, or null if not configured
   */
  protected StepConfig getStepConfig() {
    if (stepClass != null) {
      return StepConfigProvider.getStepConfig(stepClass);
    }
    return null;
  }

  /**
   * Determines whether entities should be automatically persisted before processing.
   * Override this method to enable auto-persistence.
   * 
   * @return true if entities should be auto-persisted, false otherwise
   */
  protected boolean isAutoPersistenceEnabled() {
    if (stepClass != null) {
      return StepConfigProvider.isAutoPersistenceEnabled(stepClass);
    }
    
    StepConfig config = getStepConfig();
    return config != null && config.autoPersist();
  }

  public Uni<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
    DomainIn entity = fromGrpc(grpcRequest);
    // Panache.withTransaction(...) creates the correct Vert.x context and transaction
    return Panache.withTransaction(() -> {
      Uni<DomainOut> processedResult = getService().process(entity);

      Uni<DomainOut> withPersistence = isAutoPersistenceEnabled()
              ? processedResult.onItem().call(_ ->
              // If auto-persistence is enabled, persist the input entity after successful processing
              persistenceManager.persist(entity)
      )
              : processedResult;

      if (!isAutoPersistenceEnabled()) {
        LOG.debug("Auto-persistence is disabled");
      }

      return withPersistence
              .onItem().transform(this::toGrpc)
              .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    });
  }
}
