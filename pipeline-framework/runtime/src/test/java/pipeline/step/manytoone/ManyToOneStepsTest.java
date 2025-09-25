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

import static org.junit.jupiter.api.Assertions.*;

import io.github.mbarcia.pipeline.PipelineRunner;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ManyToOneStepsTest {

    PipelineRunner pipelineRunner = new PipelineRunner();

    @Test
    void testReactiveManyToOneStep() {
        // Given: Create test entities
        TestPaymentEntity payment1 = new TestPaymentEntity("John Doe", new BigDecimal("100.00"));
        TestPaymentEntity payment2 = new TestPaymentEntity("Jane Smith", new BigDecimal("200.50"));
        TestPaymentEntity payment3 = new TestPaymentEntity("Bob Johnson", new BigDecimal("75.25"));
        TestPaymentEntity payment4 = new TestPaymentEntity("Alice Brown", new BigDecimal("300.75"));

        Multi<TestPaymentEntity> input =
                Multi.createFrom().items(payment1, payment2, payment3, payment4);

        // Create steps
        ValidatePaymentStepBlocking validateStep = new ValidatePaymentStepBlocking();
        validateStep.liveConfig().overrides().autoPersist(false);

        PaymentAggregationStep aggregateStep = new PaymentAggregationStep();
        aggregateStep.liveConfig().overrides().autoPersist(false);

        // When: Run pipeline
        AssertSubscriber<PaymentSummary> subscriber =
                pipelineRunner
                        .run(input, List.of(validateStep, aggregateStep))
                        .onItem()
                        .castTo(PaymentSummary.class)
                        .subscribe()
                        .withSubscriber(AssertSubscriber.create(2)); // Expect 2 batches

        // Then: Verify results
        subscriber.awaitItems(2).awaitCompletion(Duration.ofSeconds(10));

        List<PaymentSummary> results = subscriber.getItems();
        assertEquals(2, results.size());

        // Verify first batch
        PaymentSummary summary1 = results.getFirst();
        assertEquals(3, summary1.getTotalPayments()); // First batch of 3
        assertEquals(
                new BigDecimal("375.75"), summary1.getTotalAmount()); // 100.00 + 200.50 + 75.25

        // Verify second batch
        PaymentSummary summary2 = results.get(1);
        assertEquals(1, summary2.getTotalPayments()); // Second batch of 1
        assertEquals(new BigDecimal("300.75"), summary2.getTotalAmount()); // 300.75
    }

    @Test
    void testImperativeManyToOneStep() {
        // Given: Create test entities
        TestPaymentEntity payment1 = new TestPaymentEntity("John Doe", new BigDecimal("50.00"));
        TestPaymentEntity payment2 = new TestPaymentEntity("Jane Smith", new BigDecimal("150.25"));
        TestPaymentEntity payment3 = new TestPaymentEntity("Bob Johnson", new BigDecimal("25.50"));
        TestPaymentEntity payment4 = new TestPaymentEntity("Alice Brown", new BigDecimal("125.75"));
        TestPaymentEntity payment5 =
                new TestPaymentEntity("Charlie Wilson", new BigDecimal("80.00"));

        Multi<TestPaymentEntity> input =
                Multi.createFrom().items(payment1, payment2, payment3, payment4, payment5);

        // Create steps
        ValidatePaymentStepBlocking validateStep = new ValidatePaymentStepBlocking();
        validateStep.liveConfig().overrides().autoPersist(false);

        PaymentAggregationStepBlocking aggregateStep = new PaymentAggregationStepBlocking();
        aggregateStep.liveConfig().overrides().autoPersist(false);

        // When: Run pipeline
        AssertSubscriber<PaymentSummary> subscriber =
                pipelineRunner
                        .run(input, List.of(validateStep, aggregateStep))
                        .onItem()
                        .castTo(PaymentSummary.class)
                        .subscribe()
                        .withSubscriber(AssertSubscriber.create(2)); // Expect 2 batches

        // Then: Verify results
        subscriber.awaitItems(2).awaitCompletion(Duration.ofSeconds(10));

        List<PaymentSummary> results = subscriber.getItems();
        assertEquals(2, results.size());

        // Verify first batch
        PaymentSummary summary1 = results.getFirst();
        assertEquals(3, summary1.getTotalPayments()); // First batch of 3
        assertEquals(new BigDecimal("225.75"), summary1.getTotalAmount()); // 50.00 + 150.25 + 25.50

        // Verify second batch
        PaymentSummary summary2 = results.get(1);
        assertEquals(2, summary2.getTotalPayments()); // Second batch of 2
        assertEquals(new BigDecimal("205.75"), summary2.getTotalAmount()); // 125.75 + 80.00
    }

    // Helper step for validating payments
    public static class ValidatePaymentStepBlocking extends ConfigurableStep
            implements StepOneToOneBlocking<TestPaymentEntity, TestPaymentEntity> {

        @Override
        public TestPaymentEntity apply(TestPaymentEntity payment) {
            // Simulate some processing time
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Simple validation
            if (payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                payment.setStatus("VALIDATED");
            } else {
                payment.setStatus("REJECTED");
            }

            return payment;
        }
    }
}
