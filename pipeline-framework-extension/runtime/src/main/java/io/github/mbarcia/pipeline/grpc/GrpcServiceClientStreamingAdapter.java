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

package io.github.mbarcia.pipeline.grpc;

import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.github.mbarcia.pipeline.service.ReactiveStreamingClientService;
import io.github.mbarcia.pipeline.service.throwStatusRuntimeExceptionFunction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GrpcServiceClientStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  private static final Logger LOG = LoggerFactory.getLogger(GrpcServiceClientStreamingAdapter.class);

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

  protected abstract ReactiveStreamingClientService<DomainIn, DomainOut> getService();

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

  public Uni<GrpcOut> remoteProcess(Multi<GrpcIn> requestStream) {
    Multi<DomainIn> domainStream = requestStream.onItem().transform(this::fromGrpc);
    
    // Cache the stream so it can be subscribed to multiple times
    Multi<DomainIn> cachedStream = domainStream.cache();

    // First, process the stream without persistence
    Uni<DomainOut> processedResult = getService()
        .process(cachedStream); // Multi<DomainIn> → Uni<DomainOut>

    // If auto-persistence is enabled, persist the input entities after successful processing
    if (isAutoPersistenceEnabled()) {
      LOG.debug("Auto-persistence is enabled, will persist inputs after processing");
      
      return processedResult
          .onItem().call(result -> 
              // Persist all input items after successful processing (using cached stream)
              // This prevents duplicate persistence on retries after failures
              cachedStream
                  .onItem().transformToUniAndConcatenate(persistenceManager::persist)
                  .collect().asList() // Wait for all persist operations to complete
                  .replaceWith(result) // Replace with the originally processed result
          )
          .onItem().transform(this::toGrpc) // Uni<GrpcOut>
          .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    } else {
      LOG.debug("Auto-persistence is disabled");
      
      return processedResult
          .onItem().transform(this::toGrpc) // Uni<GrpcOut>
          .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    }
  }
}
