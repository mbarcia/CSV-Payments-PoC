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

import com.example.poc.grpc.MutinyProcessAckPaymentSentServiceGrpc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Step supplier that processes an acknowledgment payment sent.
 */
@ApplicationScoped
public class ProcessAckPaymentStep implements UniToUniStep<PaymentsProcessingSvc.AckPaymentSent, PaymentsProcessingSvc.PaymentStatus> {

    @Inject
    @GrpcClient("process-ack-payment-sent")
    MutinyProcessAckPaymentSentServiceGrpc.MutinyProcessAckPaymentSentServiceStub processAckPaymentSentService;

    @Override
    public Uni<PaymentsProcessingSvc.PaymentStatus> execute(PaymentsProcessingSvc.AckPaymentSent ack) {
        return processAckPaymentSentService.remoteProcess(ack);
    }
}