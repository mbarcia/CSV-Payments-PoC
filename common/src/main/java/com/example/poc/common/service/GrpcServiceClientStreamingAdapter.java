package com.example.poc.common.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;

public abstract class GrpcServiceClientStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

    protected abstract Service<List<DomainIn>, DomainOut> getService();

    protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

    protected abstract GrpcOut toGrpc(DomainOut domainOut);

    public Uni<GrpcOut> remoteProcess(Multi<GrpcIn> requestStream) {
        return requestStream
            .onItem().transform(this::fromGrpc)
            .collect().asList()
            .onItem().transformToUni(inputList ->
                VirtualThreadRunner.runOnVirtualThread(() -> getService().process(inputList))
                    .onItem().transform(this::toGrpc)
                    .onFailure().transform(throwable ->
                        new RuntimeException("Processing failed: " + throwable.getMessage(), throwable)
                    )
            );
    }
}
