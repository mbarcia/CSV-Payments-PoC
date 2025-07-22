package com.example.poc.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void produceVirtualThreadExecutor_shouldUseVirtualThreads() throws InterruptedException, ExecutionException, TimeoutException {
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
