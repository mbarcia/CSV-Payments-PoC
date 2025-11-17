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

public abstract class GrpcReactiveServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> extends ReactiveServiceAdapterBase<DomainIn,DomainOut> {

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

  protected abstract ReactiveService<DomainIn, DomainOut> getService();

  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  protected abstract GrpcOut toGrpc(DomainOut domainOut);

  public Uni<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
    DomainIn entity = fromGrpc(grpcRequest);
    // Panache.withTransaction(...) creates the correct Vert.x context and transaction
    return Panache.withTransaction(() -> {
      Uni<DomainOut> processedResult = getService().process(entity);

      boolean autoPersistenceEnabled = isAutoPersistenceEnabled();
      Uni<DomainOut> withPersistence = autoPersistenceEnabled
              ? processedResult.call(_ ->
              // guaranteed event-loop
              switchToEventLoop()
                  // If auto-persistence is enabled, persist the input entity after successful processing
                  .call(() -> persistenceManager.persist(entity))
              )
              : processedResult;

      if (!autoPersistenceEnabled) {
        LOG.debug("Auto-persistence is disabled");
      }

      return withPersistence
              .onItem().transform(this::toGrpc)
              .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    });
  }

}
