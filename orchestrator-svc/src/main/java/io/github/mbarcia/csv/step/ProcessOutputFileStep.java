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
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.StepManyToMany;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
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
@ApplicationScoped
@NoArgsConstructor // for CDI proxying
public class ProcessOutputFileStep extends ConfigurableStep implements StepManyToMany {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessOutputFileStep.class);

    @Inject
    @GrpcClient("process-csv-payments-output-file")
    MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub processCsvPaymentsOutputFileService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    public ProcessOutputFileStep(PipelineConfig pipelineConfig) {
        // Constructor with dependencies
        liveConfig().overrides()
                .recoverOnFailure(true)
                .debug(true)
                .autoPersist(true); // Enable auto-persistence
    }

    @Override
    public Multi<Object> applyStreaming(Multi<Object> upstream) {
        // Convert Multi<Object> to Multi<PaymentStatusSvc.PaymentOutput>
        Multi<PaymentStatusSvc.PaymentOutput> paymentStream = upstream.onItem().transform(item -> (PaymentStatusSvc.PaymentOutput) item);
        
        // Group payment outputs by their source input file path
        // This ensures that payment outputs from different input files are processed separately
        return paymentStream
            .group().by(this::getInputFilePath)
            .onItem().transformToMultiAndMerge(groupedStream -> {
                // For each group (representing one input file), process the payment outputs
                // and generate a corresponding output file
                Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> result = 
                    processCsvPaymentsOutputFileService.remoteProcess(groupedStream);
                
                // Convert Uni to Multi with a single item
                return Multi.createFrom().emitter(emitter -> result.subscribe().with(
                    file -> {
                        emitter.emit(file);
                        emitter.complete();
                    },
                    emitter::fail
                ));
            });
    }

    /**
     * Extract the input file path from a payment output.
     * This is used to group payment outputs by their source input file.
     * 
     * @param paymentOutput the payment output to extract the path from
     * @return the input file path as a string
     */
    private String getInputFilePath(PaymentStatusSvc.PaymentOutput paymentOutput) {
        try {
            // Extract the input file path from the payment output
            // This assumes the payment output contains information about its source
            if (paymentOutput.hasPaymentStatus() && 
                paymentOutput.getPaymentStatus().hasAckPaymentSent() &&
                paymentOutput.getPaymentStatus().getAckPaymentSent().hasPaymentRecord()) {

                // Return the file path as a string for grouping
                return paymentOutput.getPaymentStatus()
                    .getAckPaymentSent().getPaymentRecord().getCsvPaymentsInputFilePath();
            }
        } catch (Exception e) {
            LOG.warn("Failed to extract input file path from payment output", e);
        }
        
        // Return a default value for grouping if we can't extract the path
        return "unknown";
    }

    @Override
    public Multi<?> deadLetterMulti(Multi<Object> upstream, Throwable err) {
        LOG.error("Partial write or error occurred in CSV output processing", err);
        // Send the failed items to a dead letter queue for further processing
        return Multi.createFrom().empty();
    }
}