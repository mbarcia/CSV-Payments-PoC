package com.example.poc.common.service;

import io.smallrye.mutiny.Uni;

import java.util.concurrent.ExecutorService;

public class VirtualThreadRunner {
    private static final ExecutorService executor = VirtualThreadExecutorHolder.VIRTUAL_THREAD_EXECUTOR;

    public static <T> Uni<T> runOnVirtualThread(java.util.function.Supplier<T> blockingSupplier) {
        return Uni.createFrom().item(blockingSupplier)
                .runSubscriptionOn(executor);
    }
}
