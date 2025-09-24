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

import com.opencsv.bean.CsvToBeanBuilder;
import io.github.mbarcia.csv.common.domain.CsvPaymentsInput;
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsInputFileServiceGrpc;
import io.github.mbarcia.pipeline.GenericGrpcServiceStreamingAdapter;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.service.ReactiveStreamingService;
import io.github.mbarcia.pipeline.step.StepManyToOne;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@PipelineStep(
    order = 1,
    inputType = CsvPaymentsInput.class,
    outputType = PaymentRecord.class,
    stepType = StepManyToOne.class,
    backendType = GenericGrpcServiceStreamingAdapter.class,
    grpcStub = io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsInputFileServiceGrpc.MutinyProcessCsvPaymentsInputFileServiceStub.class,
    grpcImpl = MutinyProcessCsvPaymentsInputFileServiceGrpc.ProcessCsvPaymentsInputFileServiceImplBase.class,
    inboundMapper = io.github.mbarcia.csv.common.mapper.CsvPaymentsInputInboundMapper.class,
    outboundMapper = io.github.mbarcia.csv.common.mapper.PaymentRecordOutboundMapper.class,
    grpcClient = "process-csv-payments-input-file",
    autoPersist = true,
    debug = true
)
@ApplicationScoped
@Getter
public class ProcessCsvPaymentsInputReactiveService
    implements ReactiveStreamingService<CsvPaymentsInput, PaymentRecord> {

  Executor executor;

  @Inject
  public ProcessCsvPaymentsInputReactiveService(@Named("virtualExecutor") Executor executor) {
    this.executor = executor;
  }

  @Override
  public Multi<PaymentRecord> process(CsvPaymentsInput input) {
    Logger logger = LoggerFactory.getLogger(getClass());

    return Multi.createFrom()
        .deferred(
            Unchecked.supplier(() -> {
              try {
                var csvReader =
                    new CsvToBeanBuilder<PaymentRecord>(input.openReader())
                        .withType(PaymentRecord.class)
                        .withMappingStrategy(input.veryOwnStrategy())
                        .withSeparator(',')
                        .withIgnoreLeadingWhiteSpace(true)
                        .withIgnoreEmptyLine(true)
                        .build();

                String serviceId = this.getClass().toString();

                // Lazy + typed
                Iterator<PaymentRecord> iterator = csvReader.iterator();
                Iterable<PaymentRecord> iterable = () -> iterator;

                return Multi.createFrom()
                    .iterable(iterable)
                    .runSubscriptionOn(executor)
                    .invoke(
                        rec -> {
                          MDC.put("serviceId", serviceId);
                          logger.info("Executed command on {} --> {}", input.getSourceName(), rec);
                          MDC.clear();
                        });
              } catch (IOException e) {
                throw new RuntimeException("CSV processing error", e);
              }
            }));
  }
}
