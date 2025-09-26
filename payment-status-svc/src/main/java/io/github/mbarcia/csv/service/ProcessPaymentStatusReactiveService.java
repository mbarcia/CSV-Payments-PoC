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

import io.github.mbarcia.csv.common.domain.*;
import io.github.mbarcia.csv.common.dto.PaymentOutputDto;
import io.github.mbarcia.csv.common.mapper.PaymentOutputMapper;
import io.github.mbarcia.csv.grpc.MutinyProcessPaymentStatusServiceGrpc;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.service.ReactiveService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@PipelineStep(
    order = 5,
    inputType = io.github.mbarcia.csv.common.domain.PaymentStatus.class,
    outputType = io.github.mbarcia.csv.common.domain.PaymentOutput.class,
    stepType = io.github.mbarcia.pipeline.step.StepOneToOne.class,
    backendType = io.github.mbarcia.pipeline.GenericGrpcReactiveServiceAdapter.class,
    grpcStub = MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub.class,
    grpcImpl = MutinyProcessPaymentStatusServiceGrpc.ProcessPaymentStatusServiceImplBase.class,
    inboundMapper = io.github.mbarcia.csv.common.mapper.PaymentStatusMapper.class,
    outboundMapper = io.github.mbarcia.csv.common.mapper.PaymentOutputMapper.class,
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
