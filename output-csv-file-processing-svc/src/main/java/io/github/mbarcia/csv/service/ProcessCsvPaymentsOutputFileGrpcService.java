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

import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.common.mapper.CsvPaymentsOutputFileMapper;
import io.github.mbarcia.csv.common.mapper.PaymentOutputMapper;
import io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc;
import io.github.mbarcia.csv.grpc.OutputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class ProcessCsvPaymentsOutputFileGrpcService
    extends MutinyProcessCsvPaymentsOutputFileServiceGrpc
        .ProcessCsvPaymentsOutputFileServiceImplBase {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessCsvPaymentsOutputFileGrpcService.class);

  @Inject ProcessCsvPaymentsOutputFileReactiveService domainService;

  @Inject CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

  @Inject PaymentOutputMapper paymentOutputMapper;

  @Override
  public Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> remoteProcess(
      Multi<PaymentStatusSvc.PaymentOutput> grpcStream) {
    // Transform gRPC objects to domain objects before processing
    Multi<PaymentOutput> domainStream = grpcStream.onItem().transform(paymentOutputMapper::fromGrpc);
    
    // Use the enhanced processing method that provides completion information
    return domainService.processWithCompletion(domainStream)
        .onItem()
        .transform(result -> {
          if (result.isCompletedSuccessfully()) {
            LOG.info("CSV output file processing completed successfully with {} records", 
                result.getRecordCount());
            return csvPaymentsOutputFileMapper.toGrpc(result.getOutputFile());
          } else {
            LOG.error("CSV output file processing failed after {} records", 
                result.getRecordCount(), result.getError());
            throw new RuntimeException("CSV output file processing failed after " + 
                result.getRecordCount() + " records", result.getError());
          }
        });
  }
}
