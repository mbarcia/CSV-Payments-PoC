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

import io.github.mbarcia.csv.common.domain.CsvPaymentsInput;
import io.github.mbarcia.csv.common.domain.CsvPaymentsInputFile;
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.common.mapper.CsvPaymentsInputFileMapper;
import io.github.mbarcia.csv.common.mapper.PaymentRecordMapper;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsInputFileServiceGrpc;
import io.github.mbarcia.pipeline.grpc.GrpcServiceStreamingAdapter;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;

@GrpcService
public class ProcessCsvPaymentsInputFileGrpcService
    extends MutinyProcessCsvPaymentsInputFileServiceGrpc
        .ProcessCsvPaymentsInputFileServiceImplBase {

  @Inject
  ProcessCsvPaymentsInputReactiveService domainService;

  @Inject CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

  @Inject PaymentRecordMapper paymentRecordMapper;
  
  @Inject io.github.mbarcia.pipeline.persistence.PersistenceManager persistenceManager;

  @Override
  public Multi<InputCsvFileProcessingSvc.PaymentRecord> remoteProcess(
      InputCsvFileProcessingSvc.CsvPaymentsInputFile request) {

    GrpcServiceStreamingAdapter<
        InputCsvFileProcessingSvc.CsvPaymentsInputFile, // GrpcIn
        InputCsvFileProcessingSvc.PaymentRecord, // GrpcOut
        CsvPaymentsInput, // DomainIn
        PaymentRecord> // DomainOut
        adapter = new GrpcServiceStreamingAdapter<>() {
      @Override
      protected ProcessCsvPaymentsInputReactiveService getService() {
        return domainService;
      }

      @Override
      protected CsvPaymentsInputFile fromGrpc(
          InputCsvFileProcessingSvc.CsvPaymentsInputFile grpcIn) {
        return csvPaymentsInputFileMapper.fromGrpc(grpcIn);
      }

      @Override
      protected InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecord domainOut) {
        return paymentRecordMapper.toGrpc(paymentRecordMapper.toDto(domainOut));
      }
      
      @Override
      protected io.github.mbarcia.pipeline.config.StepConfig getStepConfig() {
        return new io.github.mbarcia.pipeline.config.StepConfig().autoPersist(true);
      }
    };
    
    // Manually inject the persistence manager since this anonymous class is not managed by CDI
    adapter.setPersistenceManager(persistenceManager);

    return adapter.remoteProcess(request);
  }
}
