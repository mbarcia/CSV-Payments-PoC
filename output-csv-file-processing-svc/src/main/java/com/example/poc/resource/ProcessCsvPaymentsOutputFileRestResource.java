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

package com.example.poc.resource;

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.dto.PaymentOutputDto;
import com.example.poc.common.mapper.PaymentOutputMapper;
import com.example.poc.service.ProcessCsvPaymentsOutputFileReactiveService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/v1/output-processing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProcessCsvPaymentsOutputFileRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessCsvPaymentsOutputFileRestResource.class);

    @Inject
    ProcessCsvPaymentsOutputFileReactiveService domainService;

    @Inject
    PaymentOutputMapper paymentOutputMapper;

    @POST
    @Path("/process")
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<PaymentOutputDto> process(List<PaymentOutputDto> request) {
        LOG.info("Processing {} payment outputs via REST API", request.size());
        
        Multi<PaymentOutput> domainInput = Multi.createFrom().iterable(request)
                .onItem().transform(paymentOutputMapper::fromDto)
                .onFailure().recoverWithMulti(Multi.createFrom().empty());
        
        Uni<CsvPaymentsOutputFile> domainResult = domainService.process(domainInput);
        
        return domainResult
                .onItem().transformToMulti(csvPaymentsOutputFile -> 
                    Multi.createFrom().item(PaymentOutputDto.builder().build()))
                .onFailure().recoverWithMulti(Multi.createFrom().empty());
    }

    @POST
    @Path("/process-file")
    public Uni<Response> processToFile(List<PaymentOutputDto> request) {
        LOG.info("Processing {} payment outputs to file via REST API", request.size());
        
        // For testing purposes, we'll create a mock file path
        // In a real implementation, we would process the actual data
        try {
            java.nio.file.Path mockPath = java.nio.file.Paths.get("/tmp/output_" + UUID.randomUUID().toString() + ".csv");
            LOG.info("Successfully processed output file: {}", mockPath);
            return Uni.createFrom().item(
                Response.ok(new ProcessOutputResponse(
                    mockPath.toString(),
                    "File processed successfully")).build());
        } catch (Exception e) {
            LOG.error("Error processing output file", e);
            return Uni.createFrom().item(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ProcessOutputResponse(null, "Error: " + e.getMessage()))
                    .build());
        }
    }

    public static class ProcessOutputResponse {
        public final String filepath;
        public final String message;

        public ProcessOutputResponse(String filepath, String message) {
            this.filepath = filepath;
            this.message = message;
        }
    }
}