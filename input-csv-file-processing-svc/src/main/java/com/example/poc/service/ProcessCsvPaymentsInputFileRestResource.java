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

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.dto.CsvPaymentsInputFileDto;
import com.example.poc.common.dto.PaymentRecordDto;
import com.example.poc.common.mapper.CsvPaymentsInputFileDtoMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.util.List;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Path("/api/v1/csv-processing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProcessCsvPaymentsInputFileRestResource {

  @Inject ProcessCsvPaymentsInputFileReactiveService domainService;

  @Inject CsvPaymentsInputFileDtoMapper csvPaymentsInputFileDtoMapper;

  @Inject PaymentRecordMapper paymentRecordMapper;

  @POST
  @Path("/process")
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<PaymentRecordDto> process(CsvPaymentsInputFileDto request) {
    CsvPaymentsInputFile domainObject = csvPaymentsInputFileDtoMapper.fromDto(request);
    Multi<PaymentRecord> domainResult = domainService.process(domainObject);
    
    return domainResult
        .map(paymentRecord -> paymentRecordMapper.toDto(paymentRecord));
  }

  @POST
  @Path("/process-list")
  public Uni<List<PaymentRecordDto>> processToList(CsvPaymentsInputFileDto request) {
    CsvPaymentsInputFile domainObject = csvPaymentsInputFileDtoMapper.fromDto(request);
    Multi<PaymentRecord> domainResult = domainService.process(domainObject);
    
    return domainResult
        .map(paymentRecord -> paymentRecordMapper.toDto(paymentRecord))
        .collect()
        .asList();
  }

  @ServerExceptionMapper
  public RestResponse handleFileNotFoundException(FileNotFoundException ex) {
    return RestResponse.ResponseBuilder
        .create(Response.Status.BAD_REQUEST)
        .entity("File not found: " + ex.getMessage())
        .build();
  }

  @ServerExceptionMapper
  public RestResponse handleRuntimeException(RuntimeException ex) {
    return RestResponse.ResponseBuilder
        .create(Response.Status.BAD_REQUEST)
        .entity("Error processing file: " + ex.getMessage())
        .build();
  }
}