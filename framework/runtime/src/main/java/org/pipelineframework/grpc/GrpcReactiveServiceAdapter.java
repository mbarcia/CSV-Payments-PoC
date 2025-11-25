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
import org.jboss.logging.Logger;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;

@SuppressWarnings("LombokSetterMayBeUsed")
public abstract class GrpcReactiveServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> extends ReactiveServiceAdapterBase {

  private static final Logger LOG = Logger.getLogger(GrpcReactiveServiceAdapter.class);

  @Inject
  PersistenceManager persistenceManager;

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
 * Provides the reactive service responsible for processing domain inputs into domain outputs.
 *
 * @return the ReactiveService that processes DomainIn to produce DomainOut.
 */
protected abstract ReactiveService<DomainIn, DomainOut> getService();

  /**
 * Convert a gRPC input object into the corresponding domain input representation.
 *
 * @param grpcIn the gRPC input object to convert
 * @return the resulting domain input object
 */
protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  /**
 * Convert a domain-layer output value into its gRPC representation.
 *
 * @param domainOut the domain-layer result to convert
 * @return the corresponding gRPC output instance
 */
protected abstract GrpcOut toGrpc(DomainOut domainOut);

  /**
   * Process a gRPC request through the reactive domain service and optionally persist the input entity.
   *
   * Converts the provided gRPC request to a domain input, invokes the underlying reactive service,
   * and converts the resulting domain output back to a gRPC response. If auto-persistence is enabled,
   * the input entity is persisted after successful processing within the correct Vert.x event-loop and transaction.
   *
   * @param grpcRequest the incoming gRPC request to convert and process
   * @return the gRPC response message corresponding to the processed domain result
   * @throws io.grpc.StatusRuntimeException if processing or persistence fails; failures are mapped to an appropriate gRPC status
   */
  public Uni<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
    DomainIn entity = fromGrpc(grpcRequest);
    // Panache.withTransaction(...) creates the correct Vert.x context and transaction
    return Panache.withTransaction(() -> {
      Uni<DomainOut> processedResult = getService().process(entity);

      boolean autoPersistenceEnabled = isAutoPersistenceEnabled();
      Uni<DomainOut> withPersistence = autoPersistenceEnabled
              ? processedResult.call(ignored ->
              // guaranteed event-loop
              switchToEventLoop()
                  // If auto-persistence is enabled, persist the input entity after successful processing
                  .call(() -> persistenceManager.persist(entity)
                          // Apply retry logic for transient database errors similar to streaming adapters
                          .onFailure(this::isTransientDbError)
                          .retry().withBackOff(java.time.Duration.ofMillis(200), java.time.Duration.ofSeconds(2)).atMost(3)
                  )
              )
              : processedResult;

      if (!autoPersistenceEnabled) {
        LOG.debug("Auto-persistence is disabled");
      } else {
        LOG.debug("Auto-persistence is enabled, will persist input after successful processing");
      }

      return withPersistence
              .onItem().transform(this::toGrpc)
              .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    });
  }

}