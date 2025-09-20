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
import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.common.mapper.AckPaymentSentMapper;
import io.github.mbarcia.csv.common.mapper.PaymentStatusMapper;
import io.github.mbarcia.csv.grpc.MutinyProcessAckPaymentSentServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.github.mbarcia.pipeline.grpc.GrpcReactiveServiceAdapter;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
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
  
  @Inject io.github.mbarcia.pipeline.persistence.PersistenceManager persistenceManager;

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
            
            @Override
            protected io.github.mbarcia.pipeline.config.StepConfig getStepConfig() {
              // For now, we'll enable auto-persistence by default for this service
              // In a real implementation, this would be configurable
              return new io.github.mbarcia.pipeline.config.StepConfig().autoPersist(true);
            }
          };

  @PostConstruct
  public void init() {
    // Manually inject the persistence manager since this anonymous class is not managed by CDI
    // This method is called after CDI has injected all dependencies
    adapter.setPersistenceManager(persistenceManager);
  }

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
