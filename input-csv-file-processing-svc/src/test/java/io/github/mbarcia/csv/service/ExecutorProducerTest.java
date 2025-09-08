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

package io.github.mbarcia.csv.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ExecutorProducerTest {

  @Test
  void testVirtualThreadExecutor() throws InterruptedException {
    ExecutorProducer producer = new ExecutorProducer();
    Executor executor = producer.produceVirtualThreadExecutor();

    assertNotNull(executor, "Executor should not be null");

    CountDownLatch latch = new CountDownLatch(1);
    final boolean[] executed = {false};

    executor.execute(
        () -> {
          executed[0] = true;
          latch.countDown();
        });

    assertTrue(latch.await(1, TimeUnit.SECONDS), "The task should have been executed");
    assertTrue(executed[0], "The task should have been executed");
  }
}
