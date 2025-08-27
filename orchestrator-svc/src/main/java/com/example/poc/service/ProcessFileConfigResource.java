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

package com.example.poc.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/process-file-config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProcessFileConfigResource {

    @Inject
    ProcessFileServiceConfig config;

    // ✅ DTO for partial updates
    public static class UpdateConfigRequest {
        public Long initialRetryDelay;
        public Integer concurrencyLimitRecords;
        public Integer maxRetries;
    }

    // ✅ GET current configuration
    @GET
    public Response getConfig() {
        return Response.ok(new UpdateConfigRequest() {{
            initialRetryDelay = config.getInitialRetryDelay();
            concurrencyLimitRecords = config.getConcurrencyLimitRecords();
            maxRetries = config.getMaxRetries();
        }}).build();
    }

    // ✅ PUT to update one or more fields
    @PUT
    public Response updateConfig(UpdateConfigRequest request) {
        if (request.initialRetryDelay != null) {
            config.setInitialRetryDelay(request.initialRetryDelay);
        }
        if (request.concurrencyLimitRecords != null) {
            config.setConcurrencyLimitRecords(request.concurrencyLimitRecords);
        }
        if (request.maxRetries != null) {
            config.setMaxRetries(request.maxRetries);
        }
        return Response.ok().build();
    }
}
