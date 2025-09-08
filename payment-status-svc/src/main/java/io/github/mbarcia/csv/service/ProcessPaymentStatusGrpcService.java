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

import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.common.mapper.PaymentOutputMapper;
import io.github.mbarcia.csv.common.mapper.PaymentStatusMapper;
import io.github.mbarcia.csv.common.service.GrpcReactiveServiceAdapter;
import io.github.mbarcia.csv.grpc.MutinyProcessPaymentStatusServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class ProcessPaymentStatusGrpcService
    extends MutinyProcessPaymentStatusServiceGrpc.ProcessPaymentStatusServiceImplBase {

  @Inject ProcessPaymentStatusReactiveService domainService;

  @Inject PaymentStatusMapper paymentStatusMapper;

  @Inject PaymentOutputMapper paymentOutputMapper;

  private final GrpcReactiveServiceAdapter<
          PaymentsProcessingSvc.PaymentStatus,
          PaymentStatusSvc.PaymentOutput,
          PaymentStatus,
          PaymentOutput>
      adapter =
          new GrpcReactiveServiceAdapter<>() {

            @Override
            protected ProcessPaymentStatusReactiveService getService() {
              return domainService;
            }

            @Override
            protected PaymentStatus fromGrpc(PaymentsProcessingSvc.PaymentStatus grpcIn) {
              return paymentStatusMapper.fromGrpc(grpcIn);
            }

            @Override
            protected PaymentStatusSvc.PaymentOutput toGrpc(PaymentOutput domainOut) {
              return paymentOutputMapper.toGrpc(domainOut);
            }
          };

  @Override
  public Uni<PaymentStatusSvc.PaymentOutput> remoteProcess(
      PaymentsProcessingSvc.PaymentStatus request) {
    return adapter.remoteProcess(request);
  }
}
