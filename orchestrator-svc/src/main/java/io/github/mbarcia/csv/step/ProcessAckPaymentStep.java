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

import io.github.mbarcia.csv.grpc.MutinyProcessAckPaymentSentServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.step.ConfigurableStepBase;
import io.github.mbarcia.pipeline.step.StepOneToAsync;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

/**
 * Step supplier that processes an acknowledgment payment sent.
 */
@ApplicationScoped
@NoArgsConstructor // for CDI proxying
public class ProcessAckPaymentStep extends ConfigurableStepBase implements StepOneToAsync<PaymentsProcessingSvc.AckPaymentSent, PaymentsProcessingSvc.PaymentStatus> {

    @Inject
    @GrpcClient("process-ack-payment-sent")
    MutinyProcessAckPaymentSentServiceGrpc.MutinyProcessAckPaymentSentServiceStub processAckPaymentSentService;

    @Inject
    public ProcessAckPaymentStep(PipelineConfig pipelineConfig) {
        // Constructor with dependencies
    }

    @Override
    public Uni<PaymentsProcessingSvc.PaymentStatus> applyAsyncUni(PaymentsProcessingSvc.AckPaymentSent ack) {
        return processAckPaymentSentService.remoteProcess(ack);
    }
}