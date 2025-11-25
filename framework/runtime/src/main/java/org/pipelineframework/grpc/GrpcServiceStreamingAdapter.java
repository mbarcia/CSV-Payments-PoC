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
import org.jboss.logging.Logger;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveStreamingService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;

/**
 * Adapter for gRPC server streaming services that handle 1-N (one-to-many) cardinality.
 * This adapter takes a single input message and returns a stream of output messages, suitable
 * for server streaming scenarios where the server sends multiple messages to the client.
 *
 * @param <GrpcIn> the gRPC input message type
 * @param <GrpcOut> the gRPC output message type
 * @param <DomainIn> the domain input object type
 * @param <DomainOut> the domain output object type
 */
@SuppressWarnings("LombokSetterMayBeUsed")
public abstract class GrpcServiceStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut>
        extends ReactiveServiceAdapterBase {

  /**
   * Default constructor for GrpcServiceStreamingAdapter.
   */
  public GrpcServiceStreamingAdapter() {
  }

  private static final Logger LOG = Logger.getLogger(GrpcServiceStreamingAdapter.class);

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

  /**
   * Gets the reactive streaming service for processing.
   *
   * @return the ReactiveStreamingService to use for processing
   */
  protected abstract ReactiveStreamingService<DomainIn, DomainOut> getService();

  /**
   * Converts a gRPC input object to the corresponding domain input object.
   *
   * @param grpcIn the gRPC input object to convert
   * @return the corresponding domain input object
   */
  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  /**
 * Convert a domain-level output object to its gRPC representation.
 *
 * @param domainOut the domain output to convert
 * @return the corresponding gRPC output
 */
protected abstract GrpcOut toGrpc(DomainOut domainOut);

  /**
   * Adapts a gRPC request into the domain stream, processes it and returns a stream of gRPC responses.
   *
   * <p>If auto-persistence is enabled the original domain input will be persisted after the stream
   * completes successfully (the persistence occurs within a transaction). Processing failures are
   * converted to gRPC status runtime exceptions.
   *
   * @param grpcRequest the incoming gRPC request to convert into a domain input
   * @return a Multi stream of gRPC responses corresponding to processed domain outputs; failures are mapped to status exceptions
   */
  public Multi<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
    DomainIn entity = fromGrpc(grpcRequest);
    Multi<DomainOut> processedResult = getService().process(entity); // Multi<DomainOut>

    if (!isAutoPersistenceEnabled()) {
      LOG.debug("Auto-persistence is disabled");
      return processedResult
              .onItem().transform(this::toGrpc)
              .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    }

    LOG.debug("Auto-persistence is enabled, will persist input after stream completes");

    return processedResult
            // After the stream completes successfully
            .onCompletion().call(() ->
                switchToEventLoop().call(() ->
                    // Panache.withTransaction(...) creates the correct Vert.x context and transaction
                    Panache.withTransaction(() ->
                            persistenceManager.persist(entity)
                                    .replaceWithVoid()
                    )
                )
            )
            // Continue with the normal outbound transformation
            .onItem().transform(this::toGrpc)
            .onFailure().transform(new throwStatusRuntimeExceptionFunction());
  }
}