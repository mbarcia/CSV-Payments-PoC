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

import com.example.poc.common.domain.CsvPaymentsInput;
import com.example.poc.common.domain.CsvPaymentsInputStream;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.mapper.CsvPaymentsInputStreamMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import com.example.poc.common.service.GrpcServiceStreamingAdapter;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.MutinyProcessCsvPaymentsInputStreamServiceGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;

@GrpcService
public class ProcessCsvPaymentsInputStreamGrpcService
    extends MutinyProcessCsvPaymentsInputStreamServiceGrpc
        .ProcessCsvPaymentsInputStreamServiceImplBase {

  @Inject
  ProcessCsvPaymentsInputReactiveService domainService;

  @Inject
  CsvPaymentsInputStreamMapper csvPaymentsInputStreamMapper;

  @Inject PaymentRecordMapper paymentRecordMapper;

  @Override
  public Multi<InputCsvFileProcessingSvc.PaymentRecord> remoteProcess(
      InputCsvFileProcessingSvc.CsvPaymentsInputStream request) {

    return new GrpcServiceStreamingAdapter<
        InputCsvFileProcessingSvc.CsvPaymentsInputStream, // GrpcIn
        InputCsvFileProcessingSvc.PaymentRecord, // GrpcOut
        CsvPaymentsInput, // DomainIn
        PaymentRecord>() // DomainOut
    {
      @Override
      protected ProcessCsvPaymentsInputReactiveService getService() {
        return domainService;
      }

      @Override
      protected CsvPaymentsInputStream fromGrpc(
          InputCsvFileProcessingSvc.CsvPaymentsInputStream grpcIn) {
        return csvPaymentsInputStreamMapper.fromGrpc(grpcIn);
      }

      @Override
      protected InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecord domainOut) {
        return paymentRecordMapper.toGrpc(paymentRecordMapper.toDto(domainOut));
      }
    }.remoteProcess(request);
  }
}
