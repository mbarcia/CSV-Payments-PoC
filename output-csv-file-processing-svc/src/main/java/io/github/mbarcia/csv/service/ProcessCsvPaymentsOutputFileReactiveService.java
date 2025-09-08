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

import io.github.mbarcia.csv.common.domain.*;
import io.github.mbarcia.csv.common.service.ReactiveStreamingClientService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Reactive service for processing streams of payment outputs and writing them to CSV files.
 * 
 * This service implements a reactive streaming pattern using Mutiny, but with a pragmatic
 * approach to file writing:
 * 1. It collects the stream into a list to work with OpenCSV's iterator-based write method
 * 2. The blocking file I/O is executed on a virtual thread to minimize resource impact
 * 3. As the terminal operation in the pipeline, it doesn't create backpressure issues
 * 
 * The service uses the iterator-based write method from OpenCSV which provides better
 * streaming characteristics than list-based writing, even though we collect the stream
 * for compatibility with the library's API.
 */
@ApplicationScoped
public class ProcessCsvPaymentsOutputFileReactiveService
    implements ReactiveStreamingClientService<PaymentOutput, CsvPaymentsOutputFile> {

  Executor executor;

  @Inject
  public ProcessCsvPaymentsOutputFileReactiveService(@Named("virtualExecutor") Executor executor) {
    this.executor = executor;
  }

  /**
   * Process a stream of payment outputs and write them to a CSV file.
   * 
   * Implementation notes:
   * - Collects the stream to a list to work with OpenCSV's iterator-based write method
   * - Uses virtual threads for the blocking file I/O operation
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
            paymentOutputs ->
                Uni.createFrom()
                    .item(
                        () -> {
                          try (CsvPaymentsOutputFile file =
                              this.getCsvPaymentsOutputFile(paymentOutputs.getFirst())) {
                            // Use iterator-based write method for better streaming characteristics
                            file.getSbc().write(paymentOutputs.iterator());
                            MDC.put("serviceId", serviceId);
                            logger.info("Executed command on stream --> {}", file.getFilepath());
                            MDC.clear();

                            return file;
                          } catch (Exception e) {
                            throw new RuntimeException("Failed to write output file.", e);
                          }
                        })
                    .runSubscriptionOn(executor));
  }

  /**
   * Create a CSV output file based on the first payment output in the stream.
   * 
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
}