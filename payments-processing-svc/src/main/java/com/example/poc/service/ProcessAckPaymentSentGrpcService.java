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

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentStatusMapper;
import com.example.poc.common.service.GrpcReactiveServiceAdapter;
import com.example.poc.grpc.MutinyProcessAckPaymentSentServiceGrpc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class ProcessAckPaymentSentGrpcService
    extends MutinyProcessAckPaymentSentServiceGrpc.ProcessAckPaymentSentServiceImplBase {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessAckPaymentSentGrpcService.class);

  @Inject ProcessAckPaymentSentReactiveService domainService;

  @Inject AckPaymentSentMapper ackPaymentSentMapper;

  @Inject PaymentStatusMapper paymentStatusMapper;

  private final GrpcReactiveServiceAdapter<
          PaymentsProcessingSvc.AckPaymentSent,
          PaymentsProcessingSvc.PaymentStatus,
          AckPaymentSent,
          PaymentStatus>
      adapter =
          new GrpcReactiveServiceAdapter<>() {
            @Override
            protected ProcessAckPaymentSentReactiveService getService() {
              return domainService;
            }

            @Override
            protected AckPaymentSent fromGrpc(PaymentsProcessingSvc.AckPaymentSent grpcIn) {
              return ackPaymentSentMapper.fromGrpc(grpcIn);
            }

            @Override
            protected PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatus domainOut) {
              return paymentStatusMapper.toGrpc(domainOut);
            }
          };

  @Override
  public Uni<PaymentsProcessingSvc.PaymentStatus> remoteProcess(
      PaymentsProcessingSvc.AckPaymentSent request) {
    LOG.debug("Received gRPC request in ProcessAckPaymentSentGrpcService: id={}, conversationId={}, paymentRecordId={}", 
        request.getId(), request.getConversationId(), request.getPaymentRecordId());
        
    Uni<PaymentsProcessingSvc.PaymentStatus> result = adapter.remoteProcess(request);
    
    LOG.debug("Returning Uni from ProcessAckPaymentSentGrpcService");
    return result
        .onItem()
        .invoke(response -> 
            LOG.debug("Successfully processed gRPC request, response: id={}, reference={}, status={}", 
                response.getId(), response.getReference(), response.getStatus()))
        .onFailure()
        .invoke(failure -> 
            LOG.error("Failed to process gRPC request in ProcessAckPaymentSentGrpcService", failure));
  }
}
