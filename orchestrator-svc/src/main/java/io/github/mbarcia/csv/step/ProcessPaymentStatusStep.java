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

package io.github.mbarcia.csv.step;

import io.github.mbarcia.csv.grpc.MutinyProcessPaymentStatusServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.StepOneToOne;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

/**
 * Step supplier that processes a payment status.
 */
@ApplicationScoped
@NoArgsConstructor // for CDI proxying
public class ProcessPaymentStatusStep extends ConfigurableStep implements StepOneToOne<PaymentsProcessingSvc.PaymentStatus, PaymentStatusSvc.PaymentOutput> {

    @Inject
    @GrpcClient("process-payment-status")
    MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub processPaymentStatusService;

    @Inject
    public ProcessPaymentStatusStep(PipelineConfig pipelineConfig) {
        // Constructor with dependencies
    }

    @Override
    public Uni<PaymentStatusSvc.PaymentOutput> applyAsyncUni(PaymentsProcessingSvc.PaymentStatus status) {
        return processPaymentStatusService.remoteProcess(status);
    }

    @Override
    public boolean runWithVirtualThreads() {
        return true;
    }
}