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

package io.github.mbarcia.pipeline.step.future;

import io.github.mbarcia.pipeline.step.ConfigurableStep;
import java.util.concurrent.CompletableFuture;

/** Example of a Future-based step that processes a payment asynchronously. */
public class ProcessPaymentFutureStep extends ConfigurableStep
    implements StepOneToOneCompletableFuture<String, String> {

  @Override
  public CompletableFuture<String> applyAsync(String paymentRequest) {
    // This returns a CompletableFuture that simulates async processing
    return CompletableFuture.supplyAsync(
        () -> {
          // Simulate some async processing time
          try {
            Thread.sleep(200); // Simulate async work
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }

          // Process the payment request
          return "Processed: " + paymentRequest;
        },
        getExecutor());
  }
}
