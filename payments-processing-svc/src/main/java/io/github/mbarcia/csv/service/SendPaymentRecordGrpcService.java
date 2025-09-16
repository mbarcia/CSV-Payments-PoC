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

import io.github.mbarcia.csv.common.domain.AckPaymentSent;
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.common.mapper.AckPaymentSentMapper;
import io.github.mbarcia.csv.common.mapper.PaymentRecordMapper;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.MutinySendPaymentRecordServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.github.mbarcia.pipeline.grpc.GrpcReactiveServiceAdapter;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class SendPaymentRecordGrpcService
    extends MutinySendPaymentRecordServiceGrpc.SendPaymentRecordServiceImplBase {

  @Inject SendPaymentRecordReactiveService domainService;

  @Inject PaymentRecordMapper paymentRecordMapper;

  @Inject AckPaymentSentMapper ackPaymentSentMapper;
  
  @Inject io.github.mbarcia.pipeline.persistence.PersistenceManager persistenceManager;

  @Override
  public Uni<PaymentsProcessingSvc.AckPaymentSent> remoteProcess(
      InputCsvFileProcessingSvc.PaymentRecord request) {

    GrpcReactiveServiceAdapter<
        InputCsvFileProcessingSvc.PaymentRecord, // GrpcIn
        PaymentsProcessingSvc.AckPaymentSent, // GrpcOut
        PaymentRecord, // DomainIn
        AckPaymentSent> // DomainOut
        adapter = new GrpcReactiveServiceAdapter<>() {
      @Override
      protected ReactiveService<PaymentRecord, AckPaymentSent> getService() {
        return domainService;
      }

      @Override
      protected PaymentRecord fromGrpc(InputCsvFileProcessingSvc.PaymentRecord grpcIn) {
        return paymentRecordMapper.fromGrpc(grpcIn);
      }

      @Override
      protected PaymentsProcessingSvc.AckPaymentSent toGrpc(AckPaymentSent domainOut) {
        return ackPaymentSentMapper.toGrpc(domainOut);
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
