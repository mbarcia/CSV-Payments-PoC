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

package org.pipelineframework.csv.service;

import com.opencsv.bean.CsvToBeanBuilder;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executor;
import lombok.Getter;
import org.jboss.logging.Logger;
import org.pipelineframework.annotation.PipelineStep;
import org.pipelineframework.csv.common.domain.CsvPaymentsInputFile;
import org.pipelineframework.csv.common.domain.PaymentRecord;
import org.pipelineframework.csv.common.mapper.CsvPaymentsInputFileMapper;
import org.pipelineframework.csv.common.mapper.PaymentRecordMapper;
import org.pipelineframework.csv.grpc.MutinyProcessCsvPaymentsInputFileServiceGrpc;
import org.pipelineframework.service.ReactiveStreamingService;
import org.slf4j.MDC;

@PipelineStep(
    order = 2,
    inputType = CsvPaymentsInputFile.class,
    outputType = PaymentRecord.class,
    inputGrpcType = org.pipelineframework.csv.grpc.InputCsvFileProcessingSvc.CsvPaymentsInputFile.class,
    outputGrpcType = org.pipelineframework.csv.grpc.InputCsvFileProcessingSvc.PaymentRecord.class,
    stepType = org.pipelineframework.step.StepOneToMany.class,
    backendType = org.pipelineframework.grpc.GrpcReactiveServiceAdapter.class,
    grpcStub = MutinyProcessCsvPaymentsInputFileServiceGrpc.MutinyProcessCsvPaymentsInputFileServiceStub.class,
    grpcImpl = MutinyProcessCsvPaymentsInputFileServiceGrpc.ProcessCsvPaymentsInputFileServiceImplBase.class,
    inboundMapper = CsvPaymentsInputFileMapper.class,
    outboundMapper = PaymentRecordMapper.class,
    grpcClient = "process-csv-payments-input-file",
    autoPersist = true,
    parallel = true,
    backpressureBufferCapacity = 10000, // the default is only 128
    debug = true
)
@ApplicationScoped
@Getter
public class ProcessCsvPaymentsInputReactiveService
    implements ReactiveStreamingService<CsvPaymentsInputFile, PaymentRecord> {

  Executor executor;

  @Inject
  public ProcessCsvPaymentsInputReactiveService(@Named("virtualExecutor") Executor executor) {
    this.executor = executor;
  }

  @Override
  public Multi<PaymentRecord> process(CsvPaymentsInputFile input) {
    Logger logger = Logger.getLogger(getClass());

    return Multi.createFrom()
        .deferred(
            Unchecked.supplier(
                () -> {
                  try {
                    var reader = input.openReader();
                    var csvReader =
                        new CsvToBeanBuilder<PaymentRecord>(reader)
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
                        .onItem()
                        .invoke(
                            rec -> {
                              MDC.put("serviceId", serviceId);
                              logger.infof(
                                  "Executed command on %s --> %s", input.getSourceName(), rec);
                              MDC.remove("serviceId");
                        })
                        .onTermination()
                        .invoke(() -> {
                          try {
                            reader.close();
                          } catch (IOException e) {
                            logger.warnf("Failed to close CSV reader", e);
                          }
                        });
                  } catch (IOException e) {
                    throw new RuntimeException("CSV processing error", e);
                  }
                }));
  }
}
