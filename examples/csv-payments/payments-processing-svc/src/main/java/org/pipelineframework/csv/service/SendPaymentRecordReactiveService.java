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
import jakarta.inject.Inject;
import lombok.Getter;
import org.pipelineframework.annotation.PipelineStep;
import org.pipelineframework.csv.common.domain.AckPaymentSent;
import org.pipelineframework.csv.common.domain.PaymentRecord;
import org.pipelineframework.csv.common.mapper.AckPaymentSentMapper;
import org.pipelineframework.csv.common.mapper.PaymentRecordMapper;
import org.pipelineframework.csv.common.mapper.SendPaymentRequestMapper;
import org.pipelineframework.csv.grpc.MutinySendPaymentRecordServiceGrpc;
import org.pipelineframework.service.ReactiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@PipelineStep(
  order = 3,
  inputType = PaymentRecord.class,
  outputType = AckPaymentSent.class,
  inputGrpcType = org.pipelineframework.csv.grpc.InputCsvFileProcessingSvc.PaymentRecord.class,
  outputGrpcType = org.pipelineframework.csv.grpc.PaymentsProcessingSvc.AckPaymentSent.class,
  stepType = org.pipelineframework.step.StepOneToOne.class,
  backendType = org.pipelineframework.grpc.GrpcReactiveServiceAdapter.class,
  grpcStub = MutinySendPaymentRecordServiceGrpc.MutinySendPaymentRecordServiceStub.class,
  grpcImpl = MutinySendPaymentRecordServiceGrpc.SendPaymentRecordServiceImplBase.class,
  inboundMapper = PaymentRecordMapper.class,
  outboundMapper = AckPaymentSentMapper.class,
  grpcClient = "send-payment-record",
  autoPersist = true,
  debug = true
)
@ApplicationScoped
@Getter
public class SendPaymentRecordReactiveService
    implements ReactiveService<PaymentRecord, AckPaymentSent> {
  private final PaymentProviderServiceMock paymentProviderServiceMock;

  @Inject
  public SendPaymentRecordReactiveService(PaymentProviderServiceMock paymentProviderServiceMock) {
    this.paymentProviderServiceMock = paymentProviderServiceMock;
  }

  @Override
  public Uni<AckPaymentSent> process(PaymentRecord paymentRecord) {
    SendPaymentRequestMapper.SendPaymentRequest request =
        new SendPaymentRequestMapper.SendPaymentRequest()
            .setAmount(paymentRecord.getAmount())
            .setReference(paymentRecord.getRecipient())
            .setCurrency(paymentRecord.getCurrency())
            .setPaymentRecord(paymentRecord)
            .setPaymentRecordId(paymentRecord.getId());

    Uni<AckPaymentSent> result =
        Uni.createFrom().item(paymentProviderServiceMock.sendPayment(request));

    String serviceId = this.getClass().toString();
    MDC.put("serviceId", serviceId);
    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info("Executed command on {} --> {}", paymentRecord, result);
    MDC.clear();

    return result;
  }
}
