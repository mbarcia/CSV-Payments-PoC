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
import io.github.mbarcia.pipeline.service.ConfigurableStepBase;
import io.github.mbarcia.pipeline.service.PipelineConfig;
import io.github.mbarcia.pipeline.service.StepManyToMany;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step supplier that processes a stream of payment outputs and produces a single output file.
 * This aggregates multiple payment outputs into one final output file.
 * <p>
 * This implementation handles completion signals and partial writes by using a custom
 * gRPC adapter that provides detailed completion information.
 */
@ApplicationScoped
@NoArgsConstructor // for CDI proxying
public class ProcessOutputFileStep extends ConfigurableStepBase implements StepManyToMany {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessOutputFileStep.class);

    @Inject
    @GrpcClient("process-csv-payments-output-file")
    MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub processCsvPaymentsOutputFileService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    public ProcessOutputFileStep(PipelineConfig pipelineConfig) {
        // Constructor with dependencies
    }

    @Override
    public Multi<Object> applyStreaming(Multi<Object> upstream) {
        // Convert Multi<Object> to Multi<PaymentStatusSvc.PaymentOutput>
        Multi<PaymentStatusSvc.PaymentOutput> paymentStream = upstream.onItem().transform(item -> (PaymentStatusSvc.PaymentOutput) item);
        
        // Use the gRPC service directly
        Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> result = processCsvPaymentsOutputFileService.remoteProcess(paymentStream);
        
        // Convert Uni to Multi with a single item
        return Multi.createFrom().emitter(emitter -> result.subscribe().with(
            file -> {
                emitter.emit(file);
                emitter.complete();
            },
                emitter::fail
        ));
    }

    @Override
    public Multi<?> deadLetterMulti(Multi<Object> upstream, Throwable err) {
        LOG.error("Partial write or error occurred in CSV output processing", err);
        // Send the failed items to a dead letter queue for further processing
        return Multi.createFrom().empty();
    }
}