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

import io.github.mbarcia.csv.common.mapper.AckPaymentSentMapper;
import io.github.mbarcia.csv.common.mapper.PaymentStatusMapper;
import io.github.mbarcia.csv.common.mapper.SendPaymentRequestMapper;
import io.github.mbarcia.csv.grpc.MutinyPaymentProviderServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PaymentProviderGrpcService
    extends MutinyPaymentProviderServiceGrpc.PaymentProviderServiceImplBase {

  @Inject PaymentProviderService domainService;

  @Inject SendPaymentRequestMapper sendPaymentRequestMapper;

  @Inject AckPaymentSentMapper ackPaymentSentMapper;

  @Inject PaymentStatusMapper paymentStatusMapper;

  @Override
  public Uni<PaymentsProcessingSvc.AckPaymentSent> sendPayment(
      PaymentStatusSvc.SendPaymentRequest grpcRequest) {
    return Uni.createFrom()
        .item(
            () -> {
              var domainIn = sendPaymentRequestMapper.fromGrpc(grpcRequest);
              var domainOut = domainService.sendPayment(domainIn);
              return ackPaymentSentMapper.toGrpc(domainOut);
            })
        .onFailure()
        .transform(
            throwable -> {
              Metadata metadata = new Metadata();
              metadata.put(
                  Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER),
                  "Error in gRPC server: " + throwable.getMessage());
              return new StatusRuntimeException(
                  Status.INTERNAL.withDescription(throwable.getMessage()).withCause(throwable),
                  metadata);
            });
  }

  @Override
  public Uni<PaymentsProcessingSvc.PaymentStatus> getPaymentStatus(
      PaymentsProcessingSvc.AckPaymentSent grpcRequest) {
    return Uni.createFrom()
        .emitter(
            emitter -> {
              try {
                var domainIn = ackPaymentSentMapper.fromGrpc(grpcRequest);
                var domainOut = domainService.getPaymentStatus(domainIn);
                var grpcResponse = paymentStatusMapper.toGrpc(domainOut);
                emitter.complete(grpcResponse);
              } catch (Exception e) {
                emitter.fail(
                    io.grpc.Status.INTERNAL
                        .withDescription("Processing failed: " + e.getMessage())
                        .withCause(e)
                        .asRuntimeException());
              }
            });
  }
}
