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
import jakarta.inject.Inject;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveBidirectionalStreamingService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for gRPC bidirectional streaming services that handle N-N (many-to-many) cardinality.
 * This adapter takes a stream of input messages and returns a stream of output messages,
 * suitable for bidirectional streaming scenarios where both client and server can send
 * multiple messages to each other.
 */
public abstract class GrpcServiceBidirectionalStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  private static final Logger LOG = LoggerFactory.getLogger(GrpcServiceBidirectionalStreamingAdapter.class);

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

  protected abstract ReactiveBidirectionalStreamingService<DomainIn, DomainOut> getService();

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

  /**
   * Process a bidirectional stream where multiple input messages are received
   * and multiple output messages are produced, handling N-N (many-to-many) cardinality.
   * This method handles bidirectional gRPC streaming where both client and server
   * can send multiple messages to each other.
   * 
   * @param requestStream the stream of input requests
   * @return a stream of output responses
   */
  public Multi<GrpcOut> remoteProcess(Multi<GrpcIn> requestStream) {
    Multi<DomainIn> domainStream = requestStream.onItem().transform(this::fromGrpc);
    
    // Cache the stream so it can be subscribed to multiple times if needed
    Multi<DomainIn> cachedStream = domainStream.cache();

    // Process the stream to produce a stream of outputs
    Multi<DomainOut> processedStream = getService()
        .process(cachedStream); // Multi<DomainIn> → Multi<DomainOut>

    // If auto-persistence is enabled, persist the input entities after successful processing
    if (isAutoPersistenceEnabled()) {
      LOG.debug("Auto-persistence is enabled, will persist inputs after processing");
      
      // Subscribe to the cached stream to persist each input item after the whole stream has been processed
      // We don't block the main stream while doing persistence
      cachedStream
          .onItem().transformToUniAndConcatenate(persistenceManager::persist)
          .onFailure().recoverWithItem((Throwable t) -> {
              LOG.warn("Failed to persist input item: {}", t.getMessage());
              return null;
          })
          .filter(item -> item != null) // Filter out null items after recovery
          .subscribe().with(
              item -> LOG.debug("Persisted input item: {}", item),
              failure -> LOG.warn("Persistence failed for item: {}", failure.getMessage())
          );
      
      return processedStream
          .onItem().transform(this::toGrpc) // Multi<GrpcOut>
          .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    } else {
      LOG.debug("Auto-persistence is disabled");
      
      return processedStream
          .onItem().transform(this::toGrpc) // Multi<GrpcOut>
          .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    }
  }
}