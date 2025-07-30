package com.example.poc.service;

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
