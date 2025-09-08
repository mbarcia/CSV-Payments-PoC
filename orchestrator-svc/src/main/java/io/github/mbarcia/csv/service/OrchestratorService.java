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

import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.common.domain.CsvPaymentsOutputFile;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class OrchestratorService {

  public static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

  @Inject HybridResourceLoader resourceLoader;

  @Inject ProcessFolderService processFolderService;
  
  @Inject ProcessInputFileStep processInputFileStep;
  
  @Inject ProcessOutputFileStep processOutputFileStep;
  
  @Inject PersistAndSendPaymentStep persistAndSendPaymentStep;
  
  @Inject ProcessAckPaymentStep processAckPaymentStep;
  
  @Inject ProcessPaymentStatusStep processPaymentStatusStep;
  
  @Inject ProcessPipelineConfig config;
  
  public Uni<Void> process(String csvFolderPath) throws URISyntaxException {
    Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> inputOutputFiles = processFolderService.process(csvFolderPath);
    
    // Create a single pipeline for processing payment records
    List<PipelineStep<?, ?>> paymentProcessingSteps = List.of(
        persistAndSendPaymentStep,
        processAckPaymentStep,
        processPaymentStatusStep
    );
    
    GenericPipelineService<InputCsvFileProcessingSvc.PaymentRecord, PaymentStatusSvc.PaymentOutput> paymentPipeline = 
        new GenericPipelineService<>(
            VIRTUAL_EXECUTOR,
            config,
            paymentProcessingSteps
        );
    
    List<Uni<CsvPaymentsOutputFile>> processingUnis =
        inputOutputFiles.keySet().stream()
            .map(inputFile -> {

                // First, process the input file to get a stream of PaymentRecord objects
                Uni<Multi<InputCsvFileProcessingSvc.PaymentRecord>> paymentRecordsUni =
                    processInputFileStep.execute(inputFile);

                // Then, for each PaymentRecord, process it through the payment pipeline
                Multi<PaymentStatusSvc.PaymentOutput> paymentOutputsMulti =
                    paymentRecordsUni
                        .onItem()
                        .transformToMulti(records -> records)
                        .onItem()
                        .transformToUniAndMerge(paymentPipeline::process);

                // Finally, process the output file with all the payment outputs
                return processOutputFileStep.execute(paymentOutputsMulti);
            })
            .toList();

    if (!processingUnis.isEmpty()) {
      return Uni.combine().all().unis(processingUnis).discardItems();
    } else {
      return Uni.createFrom().voidItem();
    }
  }
}