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

package io.github.mbarcia.csv.service;

import io.github.mbarcia.csv.common.domain.AckPaymentSent;
import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.grpc.MutinyProcessAckPaymentSentServiceGrpc;
import io.github.mbarcia.pipeline.GenericGrpcReactiveServiceAdapter;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PipelineStep(
        order = 4,
        inputType = AckPaymentSent.class,
        outputType = PaymentStatus.class,
        inputGrpcType = io.github.mbarcia.csv.grpc.PaymentsProcessingSvc.AckPaymentSent.class,
        outputGrpcType = io.github.mbarcia.csv.grpc.PaymentsProcessingSvc.PaymentStatus.class,
        stepType = io.github.mbarcia.pipeline.step.StepOneToOne.class,
        backendType = GenericGrpcReactiveServiceAdapter.class,
        grpcStub = MutinyProcessAckPaymentSentServiceGrpc.ProcessAckPaymentSentServiceImplBase.class,
        grpcImpl = MutinyProcessAckPaymentSentServiceGrpc.ProcessAckPaymentSentServiceImplBase.class,
        inboundMapper = io.github.mbarcia.csv.common.mapper.AckPaymentSentMapper.class,
        outboundMapper = io.github.mbarcia.csv.common.mapper.PaymentStatusMapper.class,
        grpcClient = "process-ack-payment-sent",
        autoPersist = true,
        debug = true
)
@ApplicationScoped
@Getter
public class ProcessAckPaymentSentReactiveService
    implements ReactiveService<AckPaymentSent, PaymentStatus> {
    
  private static final Logger LOG = LoggerFactory.getLogger(ProcessAckPaymentSentReactiveService.class);

  private final PollAckPaymentSentReactiveService pollAckPaymentSentService;

  @Inject
  public ProcessAckPaymentSentReactiveService(
      PollAckPaymentSentReactiveService pollAckPaymentSentService) {
    this.pollAckPaymentSentService = pollAckPaymentSentService;
    LOG.debug("ProcessAckPaymentSentReactiveService initialized");
  }

  @Override
  public Uni<PaymentStatus> process(AckPaymentSent ackPaymentSent) {
    return process(ackPaymentSent, true); // Default to using virtual threads
  }

  /**
   * Process with optional virtual thread execution
   * @param ackPaymentSent the payment to process
   * @param useVirtualThreads whether to use virtual threads (false for REST calls)
   * @return Uni with the payment status
   */
  public Uni<PaymentStatus> process(AckPaymentSent ackPaymentSent, boolean useVirtualThreads) {
    LOG.debug("Processing AckPaymentSent in ProcessAckPaymentSentReactiveService: id={}, conversationId={}, paymentRecordId={}", 
        ackPaymentSent.getId(), ackPaymentSent.getConversationId(), ackPaymentSent.getPaymentRecordId());
    
    // Call the service with the appropriate threading option
    Uni<PaymentStatus> result = pollAckPaymentSentService.process(ackPaymentSent, useVirtualThreads);
    
    LOG.debug("Returning Uni from ProcessAckPaymentSentReactiveService");
    return result
        .onItem()
        .invoke(paymentStatus -> 
            LOG.debug("Successfully processed AckPaymentSent, resulting PaymentStatus: id={}, reference={}, status={}", 
                paymentStatus.getId(), paymentStatus.getReference(), paymentStatus.getStatus()))
        .onFailure()
        .invoke(failure -> 
            LOG.error("Failed to process AckPaymentSent in ProcessAckPaymentSentReactiveService", failure));
  }
}
