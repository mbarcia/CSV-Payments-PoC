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

package io.github.mbarcia.csv.resource;

import io.github.mbarcia.csv.common.domain.CsvPaymentsInput;
import io.github.mbarcia.csv.common.domain.CsvPaymentsInputStream;
import io.github.mbarcia.csv.common.dto.PaymentRecordDto;
import io.github.mbarcia.csv.common.mapper.PaymentRecordMapper;
import io.github.mbarcia.csv.service.ProcessCsvPaymentsInputReactiveService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Path("/api/v1/input-processing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class ProcessCsvPaymentsInputStreamRestResource {

  @Inject
  ProcessCsvPaymentsInputReactiveService domainService;

  PaymentRecordMapper paymentRecordMapper = PaymentRecordMapper.INSTANCE;

  @POST
  @Path("/process")
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<PaymentRecordDto> process(
      @RestForm("file") InputStream file,
      @RestForm("filename") String filename) {

    CsvPaymentsInput domainObject = new CsvPaymentsInputStream(
            file,
            filename
        );

    return domainService.process(domainObject)
            .map(paymentRecordMapper::toDto)
            // Catch exceptions in the reactive stream
            .onFailure().recoverWithMulti(ex -> Multi.createFrom().empty());
  }

  @ServerExceptionMapper
  public RestResponse<String> handleRuntimeException(RuntimeException ex) {
    return RestResponse.status(Response.Status.BAD_REQUEST, "Error processing stream: " + ex.getMessage());
  }
}