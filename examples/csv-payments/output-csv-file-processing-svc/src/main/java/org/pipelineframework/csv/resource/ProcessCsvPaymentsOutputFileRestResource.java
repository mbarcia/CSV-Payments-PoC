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

package org.pipelineframework.csv.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.pipelineframework.csv.common.domain.CsvPaymentsOutputFile;
import org.pipelineframework.csv.common.domain.PaymentOutput;
import org.pipelineframework.csv.common.dto.PaymentOutputDto;
import org.pipelineframework.csv.common.mapper.PaymentOutputMapper;
import org.pipelineframework.csv.service.ProcessCsvPaymentsOutputFileReactiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("BlockingMethodInNonBlockingContext")
@Path("/api/v1/output-processing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProcessCsvPaymentsOutputFileRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessCsvPaymentsOutputFileRestResource.class);

    @Inject
    ProcessCsvPaymentsOutputFileReactiveService domainService;

    PaymentOutputMapper paymentOutputMapper = PaymentOutputMapper.INSTANCE;

    @POST
    @Path("/process")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> processAndDownload(List<PaymentOutputDto> request) {
        LOG.info("Processing {} payment outputs and preparing file download via REST API", request.size());
        
        Multi<PaymentOutput> domainInput = Multi.createFrom().iterable(request)
                .onItem().transform(paymentOutputMapper::fromDto)
                .onFailure().recoverWithMulti(Multi.createFrom().empty());
        
        Uni<CsvPaymentsOutputFile> domainResult = domainService.process(domainInput);
        
        return domainResult
                .onItem().transformToUni(file -> {
                    if (file != null) {
                        return this.createFileDownloadResponse(file);
                    } else {
                        // Return an empty response for empty streams
                        return Uni.createFrom().item(
                            Response.ok()
                                .header("Content-Disposition", "attachment; filename=\"empty.csv\"")
                                .header("Content-Type", "text/csv")
                                .entity("")
                                .build());
                    }
                })
                .onFailure().recoverWithUni(this::handleProcessingError);
    }

    @POST
    @Path("/process-file")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> processToFile(List<PaymentOutputDto> request) {
        LOG.info("Processing {} payment outputs to file via REST API", request.size());
        
        Multi<PaymentOutput> domainInput = Multi.createFrom().iterable(request)
                .onItem().transform(paymentOutputMapper::fromDto)
                .onFailure().recoverWithMulti(Multi.createFrom().empty());
        
        Uni<CsvPaymentsOutputFile> domainResult = domainService.process(domainInput);
        
        return domainResult
                .onItem().transform(file -> {
                    if (file != null) {
                        return Response.ok(new ProcessOutputResponse(
                            file.getFilepath().toString(),
                            "File processed successfully"))
                        .build();
                    } else {
                        return Response.ok(new ProcessOutputResponse(
                            null,
                            "No payment outputs to process"))
                        .build();
                    }
                })
                .onFailure().recoverWithUni(this::handleProcessingError);
    }

    private Uni<Response> createFileDownloadResponse(CsvPaymentsOutputFile file) {
        java.nio.file.Path filePath = file.getFilepath();
        String fileName = filePath.getFileName().toString();
        
        // Create streaming output for file content
        StreamingOutput output = outputStream -> {
            try {
                Files.copy(filePath, outputStream);
            } finally {
                // Clean up the temporary file after streaming
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    LOG.warn("Failed to delete temporary file: {}", filePath, e);
                }
            }
        };
        
        return Uni.createFrom().item(
            Response.ok(output)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .header("Content-Type", "text/csv")
                .build());
    }

    private Uni<Response> handleProcessingError(Throwable throwable) {
        LOG.error("Error processing payment outputs", throwable);
        return Uni.createFrom().item(
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ProcessOutputResponse(null, "Error processing payment outputs: " + throwable.getMessage()))
                .build());
    }

    public record ProcessOutputResponse(String filepath, String message) {
    }
}