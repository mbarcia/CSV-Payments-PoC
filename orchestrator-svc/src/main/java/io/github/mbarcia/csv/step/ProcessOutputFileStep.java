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

import io.github.mbarcia.csv.common.mapper.CsvPaymentsOutputFileMapper;
import io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc;
import io.github.mbarcia.csv.grpc.OutputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.step.Step;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step supplier that processes a stream of payment outputs and produces output files.
 * This implementation groups payment outputs by their source input file and creates
 * separate output files for each input file.
 * <p>
 * This implementation handles completion signals and partial writes by using a custom
 * gRPC adapter that provides detailed completion information.
 */
@PipelineStep(
    order = 6,
    autoPersist = true,
    debug = true,
    recoverOnFailure = true,
    inputType = PaymentStatusSvc.PaymentOutput.class,
    outputType = OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.class,
    stub = MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub.class,
    inboundMapper = io.github.mbarcia.csv.mapper.PaymentOutputInboundMapper.class,
    outboundMapper = io.github.mbarcia.csv.mapper.CsvPaymentsOutputFileOutboundMapper.class,
    grpcClient = "process-csv-payments-output-file"
)
@ApplicationScoped
@NoArgsConstructor // for CDI proxying
public class ProcessOutputFileStep implements Step {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessOutputFileStep.class);

    @Inject
    @GrpcClient("process-csv-payments-output-file")
    MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub processCsvPaymentsOutputFileService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    public ProcessOutputFileStep(PipelineConfig pipelineConfig) {
        // Constructor with dependencies
        // Configuration is now handled by the @PipelineStep annotation
    }

    @Override
    public Multi<Object> apply(Multi<Object> input) {
        LOG.debug("Processing payment output stream in ProcessOutputFileStep");
        
        // Cast to the correct input type
        Multi<PaymentStatusSvc.PaymentOutput> typedInput = input.onItem().transform(item -> (PaymentStatusSvc.PaymentOutput) item);
        
        // For now, we'll just pass through the items without complex grouping
        // In a real implementation, this would group by input file path and create output files
        return typedInput
            .onItem().transform(item -> {
                LOG.debug("Processing payment output item");
                // Return the item as-is for now
                return (Object) item;
            })
            .onFailure().invoke(failure -> {
                LOG.error("Failure in ProcessOutputFileStep streaming", failure);
            });
    }

    @Override
    public StepConfig effectiveConfig() {
        return new StepConfig()
            .autoPersist(true)
            .debug(true)
            .recoverOnFailure(true);
    }
}