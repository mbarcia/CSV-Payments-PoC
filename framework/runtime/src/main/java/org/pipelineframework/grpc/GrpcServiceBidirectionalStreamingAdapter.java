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
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveBidirectionalStreamingService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;

/**
 * Adapter for gRPC bidirectional streaming services that handle N-N (many-to-many) cardinality.
 * This adapter takes a stream of input messages and returns a stream of output messages, suitable
 * for bidirectional streaming scenarios where both client and server can send multiple messages to
 * each other.
 *
 * @param <GrpcIn> the gRPC input message type
 * @param <GrpcOut> the gRPC output message type
 * @param <DomainIn> the domain input object type
 * @param <DomainOut> the domain output object type
 */
@SuppressWarnings("LombokSetterMayBeUsed")
public abstract class GrpcServiceBidirectionalStreamingAdapter<
    GrpcIn, GrpcOut, DomainIn, DomainOut> extends ReactiveServiceAdapterBase {

  /**
   * Default constructor for GrpcServiceBidirectionalStreamingAdapter.
   */
  public GrpcServiceBidirectionalStreamingAdapter() {
  }

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

  /**
   * Gets the reactive bidirectional streaming service for processing.
   *
   * @return the ReactiveBidirectionalStreamingService to use for processing
   */
  protected abstract ReactiveBidirectionalStreamingService<DomainIn, DomainOut> getService();

  /**
   * Converts a gRPC input object to the corresponding domain input object.
   *
   * @param grpcIn the gRPC input object to convert
   * @return the corresponding domain input object
   */
  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  /**
 * Converts a domain output object to its corresponding gRPC message representation.
 *
 * @param domainOut the domain output object to convert
 * @return the converted gRPC output message
 */
protected abstract GrpcOut toGrpc(DomainOut domainOut);

  /**
   * Adapts a bidirectional gRPC stream of incoming messages to a stream of outgoing messages by
   * converting inputs to domain objects, delegating processing to the domain service, and converting
   * results back to gRPC responses.
   *
   * <p>If auto-persistence is enabled, input domain objects are persisted after the processing
   * stream completes; any persistence failure causes the RPC to fail. Processing failures are also
   * propagated to the caller as gRPC errors.
   *
   * @param requestStream the reactive stream of incoming {@code GrpcIn} messages
   * @return a reactive stream of {@code GrpcOut} messages produced by the domain service, or a gRPC
   *         failure if processing or (when enabled) persistence fails
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
        .onCompletion().call(() ->
            // Ensure event-loop + Hibernate Reactive context
            switchToEventLoop().call(() ->
                            Panache.withTransaction(() ->
                                    cachedStream
                                            .onItem()
                                            .transformToUniAndConcatenate(persistenceManager::persist)
                                            .collect()
                                            .asList()
                                            .replaceWithVoid()
                            )
                    )
                    .onFailure(this::isTransientDbError)
                    .retry()
                    .withBackOff(Duration.ofMillis(200), Duration.ofSeconds(2))
                    .atMost(3)
        )
        .onItem().transform(this::toGrpc)
        .onFailure().transform(new throwStatusRuntimeExceptionFunction());
  }


}