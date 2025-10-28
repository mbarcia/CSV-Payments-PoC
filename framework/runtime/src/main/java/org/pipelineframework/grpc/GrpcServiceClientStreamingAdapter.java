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
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.time.Duration;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveStreamingClientService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("LombokSetterMayBeUsed")
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
    // 1️⃣ Convert incoming gRPC messages to domain objects
    Multi<DomainIn> domainStream = requestStream.onItem().transform(this::fromGrpc);

    // 2️⃣ Cache the incoming stream (so it can be re-subscribed for persistence)
    Multi<DomainIn> cachedStream = domainStream.cache();

    // 3️⃣ Process all items → single domain result
    Uni<DomainOut> processedResult = getService().process(cachedStream); // Multi<DomainIn> → Uni<DomainOut>

    if (!isAutoPersistenceEnabled()) {
      LOG.debug("Auto-persistence is disabled");
      return processedResult
              .onItem().transform(this::toGrpc)
              .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    }

    LOG.debug("Auto-persistence is enabled, will persist all inputs after successful processing");

    // 4️⃣ After processing completes successfully, persist all inputs inside one transaction
    return processedResult
            .onItem().call(result ->
                    Panache.withTransaction(() ->
                        cachedStream
                                // Persist each DomainIn sequentially inside this transaction
                                .onItem().transformToUniAndConcatenate(persistenceManager::persist)
                                .collect().asList()
                                // Replace with original result when done
                                .replaceWith(result)
                    )
                    // Optionally, retry on transient DB issues
                    .onFailure(this::isTransientDbError)
                    .retry().withBackOff(Duration.ofMillis(200), Duration.ofSeconds(2)).atMost(3)
            )
            .onItem().transform(this::toGrpc)
            .onFailure().transform(new throwStatusRuntimeExceptionFunction());
  }

  private boolean isTransientDbError(Throwable failure) {
    String msg = failure.getMessage();
    return msg != null && (
            msg.contains("connection refused") ||
                    msg.contains("connection closed") ||
                    msg.contains("timeout")
    );
  }
}

