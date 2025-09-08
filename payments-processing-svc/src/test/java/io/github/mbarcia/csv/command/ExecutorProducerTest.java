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

package io.github.mbarcia.csv.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mbarcia.csv.service.ExecutorProducer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExecutorProducerTest {

  @Test
  @DisplayName("Should produce a non-null Executor")
  void produceVirtualThreadExecutor_shouldReturnNonNullExecutor() {
    ExecutorProducer producer = new ExecutorProducer();
    ExecutorService executor = (ExecutorService) producer.produceVirtualThreadExecutor();
    assertNotNull(executor);
  }

  @Test
  @DisplayName("Should produce an Executor that uses virtual threads")
  void produceVirtualThreadExecutor_shouldUseVirtualThreads()
      throws InterruptedException, ExecutionException, TimeoutException {
    ExecutorProducer producer = new ExecutorProducer();
    ExecutorService executor = (ExecutorService) producer.produceVirtualThreadExecutor();

    // Submit a task and check if it runs on a virtual thread
    Callable<Boolean> isVirtualThread = () -> Thread.currentThread().isVirtual();

    Future<Boolean> future = executor.submit(isVirtualThread);

    Boolean result = future.get(1, TimeUnit.SECONDS);
    assertTrue(result, "The task should be executed by a virtual thread");

    // It's good practice to shut down the executor in tests if it's created within the test method
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
  }
}
