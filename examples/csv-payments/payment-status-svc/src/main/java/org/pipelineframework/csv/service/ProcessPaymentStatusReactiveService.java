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

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import lombok.Getter;
import org.pipelineframework.annotation.PipelineStep;
import org.pipelineframework.csv.common.domain.*;
import org.pipelineframework.csv.common.domain.AckPaymentSent;
import org.pipelineframework.csv.common.domain.PaymentOutput;
import org.pipelineframework.csv.common.domain.PaymentRecord;
import org.pipelineframework.csv.common.domain.PaymentStatus;
import org.pipelineframework.csv.common.dto.PaymentOutputDto;
import org.pipelineframework.csv.common.mapper.PaymentOutputMapper;
import org.pipelineframework.csv.common.mapper.PaymentStatusMapper;
import org.pipelineframework.service.ReactiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@PipelineStep(
    order = 5,
    inputType = PaymentStatus.class,
    outputType = PaymentOutput.class,
    inputGrpcType = org.pipelineframework.csv.grpc.PaymentsProcessingSvc.PaymentStatus.class,
    outputGrpcType = org.pipelineframework.csv.grpc.PaymentStatusSvc.PaymentOutput.class,
    stepType = org.pipelineframework.step.StepOneToOne.class,
    backendType = org.pipelineframework.grpc.GrpcReactiveServiceAdapter.class,
    grpcStub = org.pipelineframework.csv.grpc.MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub.class,
    grpcImpl = org.pipelineframework.csv.grpc.MutinyProcessPaymentStatusServiceGrpc.ProcessPaymentStatusServiceImplBase.class,
    inboundMapper = PaymentStatusMapper.class,
    outboundMapper = PaymentOutputMapper.class,
    grpcClient = "process-payment-status",
    autoPersist = true,
    debug = true
)
@ApplicationScoped
@Getter
public class ProcessPaymentStatusReactiveService
    implements ReactiveService<PaymentStatus, PaymentOutput> {

  PaymentOutputMapper mapper = PaymentOutputMapper.INSTANCE;

  @Override
  public Uni<PaymentOutput> process(PaymentStatus paymentStatus) {
      AckPaymentSent ackPaymentSent = paymentStatus.getAckPaymentSent();
      assert ackPaymentSent != null;
      PaymentRecord paymentRecord = ackPaymentSent.getPaymentRecord();
      assert paymentRecord != null;

    PaymentOutputDto dto =
        PaymentOutputDto.builder()
            .id(UUID.randomUUID())
            .paymentStatus(paymentStatus)
            .csvId(paymentRecord.getCsvId())
            .recipient(paymentRecord.getRecipient())
            .amount(paymentRecord.getAmount())
            .currency(paymentRecord.getCurrency())
            .conversationId(ackPaymentSent.getConversationId())
            .status(ackPaymentSent.getStatus())
            .message(paymentStatus.getMessage())
            .fee(paymentStatus.getFee())
            .build();

    return Uni.createFrom()
            .item(mapper.fromDto(dto))
            .invoke(
                result -> {
                  String serviceId = this.getClass().toString();
                  Logger logger = LoggerFactory.getLogger(this.getClass());
                  MDC.put("serviceId", serviceId);
                  logger.info("Executed command on {} --> {}", paymentStatus, result);
                  MDC.clear();
                });
  }
}
