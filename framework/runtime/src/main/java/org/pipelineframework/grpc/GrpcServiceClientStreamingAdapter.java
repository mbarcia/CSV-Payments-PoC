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
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;
import org.pipelineframework.persistence.PersistenceManager;
import org.pipelineframework.service.ReactiveStreamingClientService;
import org.pipelineframework.service.throwStatusRuntimeExceptionFunction;

/**
 * Adapter for gRPC client streaming services that handle N-1 (many-to-one) cardinality.
 * This adapter takes a stream of input messages and returns a single output message, suitable
 * for client streaming scenarios where the client sends multiple messages to the server.
 *
 * @param <GrpcIn> the gRPC input message type
 * @param <GrpcOut> the gRPC output message type
 * @param <DomainIn> the domain input object type
 * @param <DomainOut> the domain output object type
 */
@SuppressWarnings("LombokSetterMayBeUsed")
public abstract class GrpcServiceClientStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut>
        extends ReactiveServiceAdapterBase {

  /**
   * Default constructor for GrpcServiceClientStreamingAdapter.
   */

  private static final Logger LOG = Logger.getLogger(GrpcServiceClientStreamingAdapter.class);

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
   * Gets the reactive streaming client service for processing.
   *
   * @return the ReactiveStreamingClientService to use for processing
   */
  protected abstract ReactiveStreamingClientService<DomainIn, DomainOut> getService();

  /**
   * Converts a gRPC input object to the corresponding domain input object.
   *
   * @param grpcIn the gRPC input object to convert
   * @return the corresponding domain input object
   */
  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  /**
 * Convert a domain output object to its gRPC representation.
 *
 * @param domainOut the domain-layer result to convert to a gRPC message
 * @return the corresponding gRPC output message
 */
protected abstract GrpcOut toGrpc(DomainOut domainOut);

  /**
   * Orchestrates processing of a client-streaming gRPC request into a single gRPC response, optionally persisting all inputs after successful processing.
   *
   * When auto-persistence is disabled the incoming stream is forwarded directly to the domain service for processing.
   * When auto-persistence is enabled the adapter captures all domain-converted inputs in memory, passes them to the domain service,
   * and after a successful domain result persists all captured inputs in a single transaction; persistence retries on transient
   * database errors are applied. All failures are converted to a StatusRuntimeException.
   *
   * @param requestStream the incoming stream of gRPC input messages
   * @return the gRPC output message produced from the domain result
   */
  public Uni<GrpcOut> remoteProcess(Multi<GrpcIn> requestStream) {
    // 1️⃣ Convert incoming gRPC messages to domain objects
    Multi<DomainIn> domainStream = requestStream.onItem().transform(this::fromGrpc);

    if (!isAutoPersistenceEnabled()) {
      LOG.debug("Auto-persistence is disabled");
      // Pass the original streaming Multi directly to the service (no buffering)
      Uni<DomainOut> processedResult = getService().process(domainStream);
      return processedResult
              .onItem().transform(this::toGrpc)
              .onFailure().transform(new throwStatusRuntimeExceptionFunction());
    }

    LOG.debug("Auto-persistence is enabled, will persist all inputs after successful processing");

    // Capture inputs as they flow (single copy in memory)
    List<DomainIn> capturedInputs = new ArrayList<>();
    Multi<DomainIn> capturedStream = domainStream.invoke(capturedInputs::add);

    // Process all items → single domain result using the captured stream
    Uni<DomainOut> processedResult = getService().process(capturedStream); // Multi<DomainIn> → Uni<DomainOut>

    // After processing completes successfully, persist all inputs inside one transaction
    return processedResult
            .onItem().call(result ->
                switchToEventLoop().call(() ->
                    Panache.withTransaction(() ->
                        Multi.createFrom().iterable(capturedInputs)
                                // Persist each DomainIn sequentially inside this transaction
                                .onItem().transformToUniAndConcatenate(persistenceManager::persist)
                                .collect().asList()
                                // Replace with original result when done
                                .replaceWith(result)
                    )
                )
                // Optionally, retry on transient DB issues
                .onFailure(this::isTransientDbError)
                .retry().withBackOff(Duration.ofMillis(200), Duration.ofSeconds(2)).atMost(3)
            )
            .onItem().transform(this::toGrpc)
            .onFailure().transform(new throwStatusRuntimeExceptionFunction());
  }


}
