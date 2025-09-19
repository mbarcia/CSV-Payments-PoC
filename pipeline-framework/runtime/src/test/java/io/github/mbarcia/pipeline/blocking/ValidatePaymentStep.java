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

package io.github.mbarcia.pipeline.blocking;

import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.github.mbarcia.pipeline.step.blocking.StepOneToOneBlocking;
import java.math.BigDecimal;

/**
 * Example of a blocking step that processes payment entities. This step demonstrates how to use
 * StepOneToOneBlocking for blocking operations while still benefiting from the reactive pipeline
 * framework.
 */
public class ValidatePaymentStep extends ConfigurableStep
    implements StepOneToOneBlocking<TestPaymentEntity, TestPaymentEntity> {

  @Override
  public TestPaymentEntity apply(TestPaymentEntity payment) {
    // This is a blocking operation that simulates validation logic
    // In a real application, this might call external services or perform complex calculations

    // Simulate some processing time (blocking operation)
    try {
      Thread.sleep(100); // Blocking sleep to simulate work
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Perform validation
    if (payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
      payment.setStatus("VALIDATED");
    } else {
      payment.setStatus("REJECTED");
    }

    return payment;
  }
}
