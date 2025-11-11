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
import jakarta.inject.Inject;
import java.time.Duration;
import org.jboss.logging.Logger;
import org.pipelineframework.config.StepConfig;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveBidirectionalStreamingService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;

/**
 * Adapter for gRPC bidirectional streaming services that handle N-N (many-to-many) cardinality.
 * This adapter takes a stream of input messages and returns a stream of output messages, suitable
 * for bidirectional streaming scenarios where both client and server can send multiple messages to
 * each other.
 */
@SuppressWarnings("LombokSetterMayBeUsed")
public abstract class GrpcServiceBidirectionalStreamingAdapter<
    GrpcIn, GrpcOut, DomainIn, DomainOut> {

  private final Logger logger = Logger.getLogger(getClass());

  @Inject PersistenceManager persistenceManager;

  /**
   * Sets the persistence manager for this adapter. This method is useful when the adapter is not
   * managed by CDI (e.g., anonymous inner classes).
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
   * Get the step configuration for this service adapter. Override this method to provide specific
   * configuration.
   *
   * @return the step configuration, or null if not configured
   */
  protected StepConfig getStepConfig() {
    return null;
  }

  /**
   * Determines whether entities should be automatically persisted before processing. Override this
   * method to enable auto-persistence.
   *
   * @return true if entities should be auto-persisted, false otherwise
   */
  protected boolean isAutoPersistenceEnabled() {
    StepConfig config = getStepConfig();
    return config != null && config.autoPersist();
  }

  /**
   * Handles a bidirectional gRPC streaming call (N-N cardinality) where both the client and server
   * exchange multiple messages asynchronously.
   *
   * <p>This method transforms incoming {@code GrpcIn} messages into domain entities, processes them
   * through the service layer, and emits a stream of {@code GrpcOut} responses.
   *
   * <p>When auto-persistence is enabled, each input entity is persisted sequentially and reactively
   * <em>after</em> the processing begins. Unlike the non-enforced variant, <strong>any persistence
   * failure aborts the entire gRPC stream</strong> — ensuring strict data integrity between
   * processed and persisted inputs.
   *
   * <h4>Behavior summary</h4>
   *
   * <ul>
   *   <li><b>Auto-persistence enabled:</b> Each input is persisted after processing begins; if any
   *       persist operation fails, the RPC fails immediately with a gRPC {@code UNKNOWN} status.
   *   <li><b>Auto-persistence disabled:</b> Input entities are processed but not persisted.
   *   <li>Processing errors are also propagated to the client as gRPC failures.
   * </ul>
   *
   * <p>The persistence operations are executed using {@link
   * io.smallrye.mutiny.Multi#onItem().transformToUniAndConcatenate(java.util.function.Function)} to maintain
   * sequential order and backpressure safety. This guarantees that no concurrent writes occur, even
   * when processing high-volume streams.
   *
   * @param requestStream a reactive {@link Multi} of incoming {@code GrpcIn} messages.
   * @return a reactive {@link Multi} of outgoing {@code GrpcOut} messages produced by the service.
   */
  public Multi<GrpcOut> remoteProcess(Multi<GrpcIn> requestStream) {
    Multi<DomainIn> domainStream = requestStream
            .onItem()
            .transform(this::fromGrpc);

    if (!isAutoPersistenceEnabled()) {
      logger.debug("Auto-persistence is DISABLED — normal stream processing only");

      return getService().process(domainStream)
          .onItem()
          .transform(this::toGrpc)
          .onFailure()
          .transform(new throwStatusRuntimeExceptionFunction());
    }

    // Cache so we can re-consume the stream for persistence later ("hot" and "shared")
    // Only cache when auto-persistence is enabled
    Multi<DomainIn> cachedStream = domainStream.cache();

    logger.debug("Auto-persistence is ENABLED — will persist inputs after full stream completion");

    // Step 1: process the stream (N inputs → N outputs)
    // Process without caching the output to avoid unbounded memory growth
    Multi<DomainOut> processedStream = getService().process(cachedStream);

    // Step 2: After stream finishes successfully, persist all inputs (once)
    return processedStream
        // When the stream completes successfully (not per item!)
        .onCompletion()
        .call(
            () ->
                Panache.withTransaction(
                        () ->
                            cachedStream
                                .onItem()
                                .transformToUniAndConcatenate(persistenceManager::persist)
                                .collect()
                                .asList()
                                .replaceWithVoid())
                    // Optional retry for transient DB errors
                    .onFailure(this::isTransientDbError)
                    .retry()
                    .withBackOff(Duration.ofMillis(200), Duration.ofSeconds(2))
                    .atMost(3))
        .onItem()
        .transform(this::toGrpc)
        .onFailure()
        .transform(new throwStatusRuntimeExceptionFunction());
  }

  private boolean isTransientDbError(Throwable failure) {
    String msg = failure.getMessage();
    return msg != null
        && (msg.contains("connection refused")
            || msg.contains("connection closed")
            || msg.contains("timeout"));
  }
}
