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

import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.MutinySendPaymentRecordServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.StepOneToOne;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

/**
 * Step supplier that sends the payment to a third party service (mocked).
 */
@PipelineStep(
    order = 3,
    autoPersist = true,
    debug = true,
    recoverOnFailure = true,
    inputType = InputCsvFileProcessingSvc.PaymentRecord.class,
    outputType = PaymentsProcessingSvc.AckPaymentSent.class
)
@ApplicationScoped
@NoArgsConstructor // for CDI proxying
public class SendPaymentStep extends ConfigurableStep implements StepOneToOne<InputCsvFileProcessingSvc.PaymentRecord, PaymentsProcessingSvc.AckPaymentSent> {

    @Inject
    @GrpcClient("send-payment-record")
    MutinySendPaymentRecordServiceGrpc.MutinySendPaymentRecordServiceStub sendPaymentRecordService;

    @Inject
    public SendPaymentStep(PipelineConfig pipelineConfig) {
        // Constructor with dependencies
        // step-specific overrides if needed
        liveConfig().overrides()
            .recoverOnFailure(true)
            .debug(true)
            .autoPersist(true); // Enable auto-persistence
    }

    @Override
    public Uni<PaymentsProcessingSvc.AckPaymentSent> applyAsyncUni(InputCsvFileProcessingSvc.PaymentRecord in) {
        // The payment record will be automatically persisted by the pipeline framework
        // We only need to send the payment
        return sendPaymentRecordService.remoteProcess(in);
    }

    @Override
    public boolean runWithVirtualThreads() {
        return true;
    }
}