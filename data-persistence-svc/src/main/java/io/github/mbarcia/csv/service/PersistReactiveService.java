/*
 * Copyright © 2023-2025 Mariano Barcia
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

package io.github.mbarcia.csv.service;

import io.github.mbarcia.pipeline.service.ReactiveService;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public interface PersistReactiveService<T> extends ReactiveService<T, T> {
    
    PanacheRepository<T> getRepository();

    @Override
    default Uni<T> process(T processableObj) {
        // Perform the database operation
        return getRepository().persist(processableObj)
            // Log after the operation completes without blocking the reactive thread
            .invoke(() -> {
                String serviceId = this.getClass().toString();
                Logger logger = LoggerFactory.getLogger(this.getClass());
                MDC.put("serviceId", serviceId);
                logger.info("Persisted entity {} for service {}", processableObj, serviceId);
                MDC.clear();
            })
            .onFailure()
            .transform(
                throwable -> {
                    Metadata metadata = new Metadata();
                    metadata.put(
                        Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER),
                            MessageFormat.format("Error persisting {0}: {1}", processableObj, throwable.getMessage())
                    );
                    return new StatusRuntimeException(
                        Status.INTERNAL.withDescription(throwable.getMessage()).withCause(throwable),
                        metadata
                    );
                });
    }
}
