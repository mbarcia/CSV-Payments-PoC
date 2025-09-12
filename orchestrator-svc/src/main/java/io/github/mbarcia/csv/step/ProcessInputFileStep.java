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

package io.github.mbarcia.csv.step;

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.common.mapper.CsvPaymentsInputFileMapper;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsInputFileServiceGrpc;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.step.ConfigurableStepBase;
import io.github.mbarcia.pipeline.step.StepOneToMany;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step supplier that processes the input CSV file and produces a stream of payment records.
 * This converts a single input file into multiple payment records.
 */
@ApplicationScoped
@NoArgsConstructor // for CDI proxying
public class ProcessInputFileStep extends ConfigurableStepBase implements StepOneToMany<CsvPaymentsInputFile, InputCsvFileProcessingSvc.PaymentRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessInputFileStep.class);

    @Inject
    @GrpcClient("process-csv-payments-input-file")
    MutinyProcessCsvPaymentsInputFileServiceGrpc.MutinyProcessCsvPaymentsInputFileServiceStub processCsvPaymentsInputFileService;

    @Inject
    CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

    @Inject
    public ProcessInputFileStep(PipelineConfig pipelineConfig) {
        // Constructor with dependencies
    }

    @Override
    public Multi<InputCsvFileProcessingSvc.PaymentRecord> applyMulti(CsvPaymentsInputFile inputFile) {
        LOG.debug("Attempting to call processCsvPaymentsInputFileService.remoteProcess");
        Multi<InputCsvFileProcessingSvc.PaymentRecord> inputRecords =
            processCsvPaymentsInputFileService.remoteProcess(
                csvPaymentsInputFileMapper.toGrpc(inputFile))
            .onFailure()
            .invoke(e -> {
                if (e instanceof StatusRuntimeException grpcEx) {
                    LOG.error("gRPC error when calling processCsvPaymentsInputFileService: code={0}, description={}, cause={}",
                        grpcEx.getStatus().getCode(), 
                        grpcEx.getStatus().getDescription(), 
                        grpcEx.getCause());
                } else {
                    LOG.error("Non-gRPC error when calling processCsvPaymentsInputFileService", e);
                }
            });

        LOG.debug("Successfully called processCsvPaymentsInputFileService.remoteProcess");
        return inputRecords;
    }
}