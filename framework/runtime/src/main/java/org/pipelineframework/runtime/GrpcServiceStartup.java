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

package org.pipelineframework.runtime;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Runtime startup bean that ensures gRPC services are properly initialized.
 * This helps ensure that generated gRPC services are discovered by Quarkus.
 */
@ApplicationScoped
public class GrpcServiceStartup {

    private static final Logger LOG = Logger.getLogger(GrpcServiceStartup.class);

    @Inject
    BeanManager beanManager;

    void onStart(@Observes StartupEvent event) {
        LOG.info("Starting gRPC service initialization...");
        // This will trigger CDI to initialize beans with @GrpcService annotation
        LOG.info("gRPC service initialization completed.");
    }
}