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

package pipeline.step.manytoone;

import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.StepManyToOne;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.math.BigDecimal;

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
    public long batchTimeoutMs() {
        return 1000; // 1 second timeout
    }

    @Override
    public io.github.mbarcia.pipeline.config.StepConfig effectiveConfig() {
        return new io.github.mbarcia.pipeline.config.StepConfig();
    }

    @Override
    public void initialiseWithConfig(io.github.mbarcia.pipeline.config.LiveStepConfig config) {
        // Use the config provided
    }
}
