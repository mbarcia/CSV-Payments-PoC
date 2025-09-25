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

package pipeline.step.manytoone;

import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.blocking.StepManyToOneBlocking;
import java.math.BigDecimal;
import java.util.List;

/**
 * Example of a blocking many-to-one step that aggregates multiple payment entities into a summary.
 */
public class PaymentAggregationStepBlocking extends ConfigurableStep
        implements StepManyToOneBlocking<TestPaymentEntity, PaymentSummary> {

    @Override
    public PaymentSummary applyBatchList(List<TestPaymentEntity> inputs) {
        // Simulate some processing time
        try {
            Thread.sleep(100); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Aggregate payments into a summary
        BigDecimal totalAmount =
                inputs.stream()
                        .map(TestPaymentEntity::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        String summary =
                "Processed " + inputs.size() + " payments with total amount: " + totalAmount;

        PaymentSummary paymentSummary = new PaymentSummary();
        paymentSummary.setTotalPayments(inputs.size());
        paymentSummary.setTotalAmount(totalAmount);
        paymentSummary.setSummary(summary);

        return paymentSummary;
    }

    @Override
    public int batchSize() {
        return 3; // Process in batches of 3
    }

    @Override
    public long batchTimeoutMs() {
        return 1000; // 1 second timeout
    }
}
