package com.example.poc.common.service;

import io.smallrye.mutiny.Uni;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class GrpcServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    protected abstract Service<DomainIn, DomainOut> getService();
    protected abstract DomainIn fromGrpc(GrpcIn grpcIn);
    protected abstract GrpcOut toGrpc(DomainOut domainOut);

    public Uni<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
        return Uni.createFrom()
                .completionStage(CompletableFuture.supplyAsync(() -> {
                    DomainIn domainIn = fromGrpc(grpcRequest);
                    DomainOut domainOut = getService().process(domainIn);
                    return toGrpc(domainOut);
                }, VIRTUAL_EXECUTOR));
    }
}
