package com.example.poc.common.service;

import io.smallrye.mutiny.Multi;

import java.util.stream.Stream;

public abstract class GrpcServiceStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

    protected abstract Service<DomainIn, Stream<DomainOut>> getService();

    protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

    protected abstract GrpcOut toGrpc(DomainOut domainOut);

    public Multi<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
        return VirtualThreadRunner.runOnVirtualThread(() -> {
                DomainIn domainIn = fromGrpc(grpcRequest);
                return getService().process(domainIn);
            })
            .onItem().transformToMulti(stream ->
                Multi.createFrom().items(stream)
                    .onTermination().invoke(stream::close)
                    .onItem().transform(this::toGrpc)
            );
    }
}
