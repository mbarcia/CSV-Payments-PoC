package com.example.poc.command;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class ExecutorProducer {

    @Produces
    @Named("virtualExecutor")
    public Executor produceVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
