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

package io.github.mbarcia.csv.util;

import io.github.mbarcia.csv.common.domain.CsvPaymentsOutputFile;
import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.common.mapper.CsvPaymentsOutputFileMapper;
import io.github.mbarcia.csv.common.mapper.PaymentOutputMapper;
import io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc;
import io.github.mbarcia.csv.grpc.OutputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.pipeline.service.GrpcServiceClientStreamingAdapter;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom gRPC adapter that handles completion signals for CSV output file processing.
 * This adapter wraps the standard gRPC service to provide additional completion
 * and error handling for partial writes.
 */
@ApplicationScoped
public class ProcessCsvPaymentsOutputFileCompletionAdapter 
    extends GrpcServiceClientStreamingAdapter<
        PaymentStatusSvc.PaymentOutput,
        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile,
        PaymentOutput,
        CsvPaymentsOutputFile> {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessCsvPaymentsOutputFileCompletionAdapter.class);

  @Inject
  @GrpcClient("process-csv-payments-output-file")
  MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub processCsvPaymentsOutputFileService;

  @Inject
  CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

  @Inject
  PaymentOutputMapper paymentOutputMapper;

  @Override
  protected io.github.mbarcia.pipeline.service.ReactiveStreamingClientService<PaymentOutput, CsvPaymentsOutputFile> getService() {
    // This method is not used in our implementation since we're calling the gRPC service directly
    return null;
  }

  @Override
  protected PaymentOutput fromGrpc(PaymentStatusSvc.PaymentOutput grpcIn) {
    return paymentOutputMapper.fromGrpc(grpcIn);
  }

  @Override
  protected OutputCsvFileProcessingSvc.CsvPaymentsOutputFile toGrpc(CsvPaymentsOutputFile domainOut) {
    return csvPaymentsOutputFileMapper.toGrpc(domainOut);
  }

  /**
   * Process a stream of payment outputs with completion handling.
   * This method adds error handling and completion signals to the standard gRPC call.
   *
   * @param requestStream stream of payment outputs to process
   * @return Uni containing the output file information or error details
   */
  public Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> processWithCompletion(
      Multi<PaymentStatusSvc.PaymentOutput> requestStream) {
    
    return super.remoteProcess(requestStream)
        .onItemOrFailure()
        .invoke((_, failure) -> {
          if (failure != null) {
            // Handle partial write scenarios
            LOG.error("CSV output file processing failed", failure);
          } else {
            // File completed successfully
            LOG.info("CSV output file processing completed successfully");
          }
        });
  }
}