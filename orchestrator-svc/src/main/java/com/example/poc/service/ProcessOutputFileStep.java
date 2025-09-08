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

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc;
import com.example.poc.grpc.PaymentStatusSvc;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step supplier that processes a stream of payment outputs and produces a single output file.
 * This aggregates multiple payment outputs into one final output file.
 */
@ApplicationScoped
public class ProcessOutputFileStep implements MultiToUniStep<PaymentStatusSvc.PaymentOutput, CsvPaymentsOutputFile> {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessOutputFileStep.class);

    @Inject
    @GrpcClient("process-csv-payments-output-file")
    MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub processCsvPaymentsOutputFileService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Override
    public Uni<CsvPaymentsOutputFile> execute(Multi<PaymentStatusSvc.PaymentOutput> paymentOutputMulti) {
        // Now send final output for entire file
        Multi<PaymentStatusSvc.PaymentOutput> paymentOutputMultiIntermediate =
            paymentOutputMulti
                .collect()
                .asList()
                .onItem()
                .transformToMulti(Multi.createFrom()::iterable);

        LOG.debug("Attempting to call processCsvPaymentsOutputFileService.remoteProcess");
        return processCsvPaymentsOutputFileService
            .remoteProcess(paymentOutputMultiIntermediate)
            .onItem()
            .transform(csvPaymentsOutputFileMapper::fromGrpc)
            .onItem()
            .invoke(result -> LOG.info("✅ Completed processing: {}", result))
            .onFailure()
            .invoke(e -> LOG.error("❌ Processing failed: {}", e.getMessage(), e));
    }
}