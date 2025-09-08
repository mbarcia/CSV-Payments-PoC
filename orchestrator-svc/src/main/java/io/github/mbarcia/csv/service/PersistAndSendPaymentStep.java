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

import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.MutinyPersistPaymentRecordServiceGrpc;
import io.github.mbarcia.csv.grpc.MutinySendPaymentRecordServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.github.mbarcia.pipeline.service.UniToUniStep;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Step supplier that persists a payment record and sends the payment.
 */
@ApplicationScoped
public class PersistAndSendPaymentStep implements UniToUniStep<InputCsvFileProcessingSvc.PaymentRecord, PaymentsProcessingSvc.AckPaymentSent> {

    @Inject
    @GrpcClient("persist-payment-record")
    MutinyPersistPaymentRecordServiceGrpc.MutinyPersistPaymentRecordServiceStub persistPaymentRecordService;

    @Inject
    @GrpcClient("send-payment-record")
    MutinySendPaymentRecordServiceGrpc.MutinySendPaymentRecordServiceStub sendPaymentRecordService;

    @Override
    public Uni<PaymentsProcessingSvc.AckPaymentSent> execute(InputCsvFileProcessingSvc.PaymentRecord record) {
        return Uni.createFrom().item(record)
            .flatMap(persistPaymentRecordService::remoteProcess)
            .flatMap(sendPaymentRecordService::remoteProcess);
    }
}