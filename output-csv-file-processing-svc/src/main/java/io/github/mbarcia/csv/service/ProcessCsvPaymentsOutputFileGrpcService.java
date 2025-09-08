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

import io.github.mbarcia.csv.common.domain.CsvPaymentsOutputFile;
import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.common.mapper.CsvPaymentsOutputFileMapper;
import io.github.mbarcia.csv.common.mapper.PaymentOutputMapper;
import io.github.mbarcia.csv.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc;
import io.github.mbarcia.csv.grpc.OutputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.pipeline.service.GrpcServiceClientStreamingAdapter;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class ProcessCsvPaymentsOutputFileGrpcService
    extends MutinyProcessCsvPaymentsOutputFileServiceGrpc
        .ProcessCsvPaymentsOutputFileServiceImplBase {

  @Inject ProcessCsvPaymentsOutputFileReactiveService domainService;

  @Inject CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

  @Inject PaymentOutputMapper paymentOutputMapper;

  @Override
  public Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> remoteProcess(
      Multi<PaymentStatusSvc.PaymentOutput> grpcStream) {
    return new GrpcServiceClientStreamingAdapter<
        PaymentStatusSvc.PaymentOutput,
        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile,
        PaymentOutput,
        CsvPaymentsOutputFile>() {

      @Override
      protected ProcessCsvPaymentsOutputFileReactiveService getService() {
        return domainService;
      }

      @Override
      protected PaymentOutput fromGrpc(PaymentStatusSvc.PaymentOutput grpcIn) {
        return paymentOutputMapper.fromGrpc(grpcIn);
      }

      @Override
      protected OutputCsvFileProcessingSvc.CsvPaymentsOutputFile toGrpc(
          CsvPaymentsOutputFile domainOut) {
        return csvPaymentsOutputFileMapper.toGrpc(domainOut);
      }
    }.remoteProcess(grpcStream); // <-- send Multi<> input instead
  }
}
