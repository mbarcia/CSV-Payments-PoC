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

package io.github.mbarcia.csv.service;

import io.github.mbarcia.csv.common.domain.*;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.service.ReactiveStreamingClientService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Reactive service for processing streams of payment outputs and writing them to CSV files.
 * <p>
 * This service implements a reactive streaming pattern using Mutiny, but with a pragmatic
 * approach to file writing:
 * 1. It collects the stream into a list to work with OpenCSV's iterator-based write method
 * 2. File I/O operations run on the event loop thread since they're not a bottleneck
 * 3. As the terminal operation in the pipeline, it doesn't create backpressure issues
 * <p>
 * The service uses the iterator-based write method from OpenCSV which provides better
 * streaming characteristics than list-based writing, even though we collect the stream
 * for compatibility with the library's API.
 */
@PipelineStep(
  order = 6,
  inputType = io.github.mbarcia.csv.common.domain.PaymentOutput.class,
  outputType = io.github.mbarcia.csv.common.domain.CsvPaymentsOutputFile.class,
  inputGrpcType = io.github.mbarcia.csv.grpc.PaymentStatusSvc.PaymentOutput.class,
  outputGrpcType = io.github.mbarcia.csv.grpc.OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.class,
  stepType = io.github.mbarcia.pipeline.step.StepManyToOne.class,
  backendType = io.github.mbarcia.pipeline.GenericGrpcServiceClientStreamingAdapter.class,
  grpcStub = io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub.class,
  grpcImpl = io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc.ProcessCsvPaymentsOutputFileServiceImplBase.class,
  inboundMapper = io.github.mbarcia.csv.common.mapper.PaymentOutputMapper.class,
  outboundMapper = io.github.mbarcia.csv.common.mapper.CsvPaymentsOutputFileMapper.class,
  grpcClient = "process-csv-payments-output-file",
  autoPersist = true,
  debug = true,
  batchSize = 50,  // Larger batch size to ensure all related records are processed together
  batchTimeoutMs = 10000L  // 10 second timeout to allow all related records to accumulate
)
@ApplicationScoped
public class ProcessCsvPaymentsOutputFileReactiveService
    implements ReactiveStreamingClientService<PaymentOutput, CsvPaymentsOutputFile> {

  /**
   * Process a stream of payment outputs and write them to a CSV file.
   * <p>
   * Implementation notes:
   * - Collects the stream to a list to work with OpenCSV's iterator-based write method
   * - File I/O operations run on the event loop thread since they're not a bottleneck
   * - As the terminal operation in the service pipeline, it doesn't create backpressure
   * - Uses try-with-resources to ensure proper file cleanup
   * 
   * @param paymentOutputList stream of payment outputs to process
   * @return Uni containing the generated CSV file information
   */
  @Override
  public Uni<CsvPaymentsOutputFile> process(Multi<PaymentOutput> paymentOutputList) {
    Logger logger = LoggerFactory.getLogger(getClass());
    String serviceId = this.getClass().toString();

    return paymentOutputList
        .collect()
        .asList()
        .onItem()
        .transformToUni(
            paymentOutputs -> {
              // Handle empty stream case
              if (paymentOutputs.isEmpty()) {
                logger.info("No payment outputs to process");
                return Uni.createFrom().item((CsvPaymentsOutputFile) null);
              }
              
              // Track the number of records for partial write detection
              final AtomicInteger recordCount = new AtomicInteger(0);
              
              return Uni.createFrom()
                  .deferred(() -> createOutputFile(paymentOutputs.getFirst(), logger))
                  .onFailure()
                  .recoverWithUni(failure -> {
                    logger.error("Failed to create output file", failure);
                    return Uni.createFrom().failure(new RuntimeException("Failed to create output file", failure));
                  })
                  .onItem()
                  .transformToUni(file -> {
                    try {
                      // Use iterator-based write method for better streaming characteristics
                      file.getSbc().write(paymentOutputs.iterator());
                      recordCount.set(paymentOutputs.size());
                      MDC.put("serviceId", serviceId);
                      logger.info("Executed command on stream --> {} with {} records", 
                          file.getFilepath(), recordCount.get());
                      MDC.clear();
                      return Uni.createFrom().item(file);
                    } catch (Exception e) {
                      // Try to close the file before propagating the error
                      try {
                        file.close();
                      } catch (Exception closeException) {
                        logger.warn("Failed to close output file", closeException);
                      }
                      // Propagate the error
                      return Uni.createFrom().failure(new RuntimeException(e));
                    }
                  })
                  .onItem()
                  .call(file -> Uni.createFrom().item(() -> {
                    try {
                      file.close(); // Properly close the resource
                    } catch (Exception e) {
                      logger.warn("Failed to close output file", e);
                    }
                    return null;
                  }))
                  .onFailure()
                  .recoverWithUni(failure -> {
                    // Log the number of records processed before failure
                    logger.error("Failed to write output file after processing {} records", 
                        recordCount.get(), failure);
                    return Uni.createFrom().failure(new RuntimeException("Failed to write output file after processing " + 
                        recordCount.get() + " records.", failure));
                  });
            });
  }

  /**
   * Create a CSV output file based on the first payment output in the stream.
   * <p>
   * Extracts the input file path from the payment output to determine where
   * the output file should be written.
   * 
   * @param paymentOutput first payment output in the stream
   * @return CsvPaymentsOutputFile instance for writing
   * @throws IOException if there's an error creating the file
   */
  protected CsvPaymentsOutputFile getCsvPaymentsOutputFile(PaymentOutput paymentOutput)
      throws IOException {
    assert paymentOutput != null;
    PaymentStatus paymentStatus = paymentOutput.getPaymentStatus();
    assert paymentStatus != null;
    AckPaymentSent ackPaymentSent = paymentStatus.getAckPaymentSent();
    assert ackPaymentSent != null;
    PaymentRecord paymentRecord = ackPaymentSent.getPaymentRecord();
    assert paymentRecord != null;
    Path csvPaymentsInputFilePath = paymentRecord.getCsvPaymentsInputFilePath();

    return new CsvPaymentsOutputFile(csvPaymentsInputFilePath);
  }
  
  /**
   * Creates an output file and handles any IOException that might occur.
   * This method exists to properly handle the checked exception in a reactive context.
   */
  private Uni<CsvPaymentsOutputFile> createOutputFile(PaymentOutput paymentOutput, Logger logger) {
    try {
      return Uni.createFrom().item(this.getCsvPaymentsOutputFile(paymentOutput));
    } catch (IOException e) {
      logger.error("Failed to create output file due to IO error", e);
      return Uni.createFrom().failure(new RuntimeException("Failed to create output file due to IO error", e));
    }
  }
}