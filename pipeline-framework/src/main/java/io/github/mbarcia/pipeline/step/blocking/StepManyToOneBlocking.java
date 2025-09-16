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

package io.github.mbarcia.pipeline.step.blocking;

import io.github.mbarcia.pipeline.step.StepBase;
import java.util.List;

/** N -> 1 (imperative) */
public interface StepManyToOneBlocking<I, O> extends StepBase {
    /**
     * Apply the step to a batch of inputs, producing a single output.
     * 
     * @param inputs The list of inputs to process
     * @return The single output
     */
    O applyBatchList(List<I> inputs);
    
    /**
     * The batch size for collecting inputs before processing.
     * 
     * @return The batch size (default: 10)
     */
    default int batchSize() {
        return 10;
    }
    
    /**
     * The time window in milliseconds to wait before processing a batch,
     * even if the batch size hasn't been reached.
     * 
     * @return The time window in milliseconds (default: 1000ms)
     */
    default long batchTimeoutMs() {
        return 1000;
    }
    
    /**
     * Handle a failed batch by sending it to a dead letter queue or similar mechanism.
     * 
     * @param inputs The list of inputs that failed to process
     * @param error The error that occurred
     * @return The result of dead letter handling (can be null)
     */
    default O deadLetterBatchList(List<I> inputs, Throwable error) {
        System.err.printf("DLQ drop for batch of %d items: %s%n", inputs.size(), error.getMessage());
        return null;
    }
}