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

package org.pipelineframework.pipeline.step.manytoone;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.math.BigDecimal;
import java.time.Duration;
import org.pipelineframework.step.ConfigurableStep;
import org.pipelineframework.step.StepManyToOne;

/**
 * Example of a reactive many-to-one step that aggregates multiple payment entities into a summary.
 */
public class PaymentAggregationStep extends ConfigurableStep
        implements StepManyToOne<TestPaymentEntity, PaymentSummary> {

    @Override
    public Uni<PaymentSummary> applyBatchMulti(Multi<TestPaymentEntity> inputs) {
        // Simulate some processing time
        try {
            Thread.sleep(100); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Aggregate payments into a summary
        return inputs.collect()
                .asList()
                .onItem()
                .transform(
                        payments -> {
                            BigDecimal totalAmount =
                                    payments.stream()
                                            .map(TestPaymentEntity::getAmount)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                            String summary =
                                    "Processed "
                                            + payments.size()
                                            + " payments with total amount: "
                                            + totalAmount;

                            PaymentSummary paymentSummary = new PaymentSummary();
                            paymentSummary.setTotalPayments(payments.size());
                            paymentSummary.setTotalAmount(totalAmount);
                            paymentSummary.setSummary(summary);

                            return paymentSummary;
                        });
    }

    @Override
    public int batchSize() {
        return 3; // Process in batches of 3
    }

    @Override
    public Duration batchTimeout() {
        return Duration.ofMillis(1000); // 1 second timeout
    }

    @Override
    public org.pipelineframework.config.StepConfig effectiveConfig() {
        return new org.pipelineframework.config.StepConfig();
    }

    @Override
    public void initialiseWithConfig(org.pipelineframework.config.LiveStepConfig config) {
        // Use the config provided
    }
}
