package com.example.poc.common.service;

import io.smallrye.mutiny.Multi;

import java.util.stream.Stream;

public abstract class GrpcServiceStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

    protected abstract Service<DomainIn, Stream<DomainOut>> getService();

    protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

    protected abstract GrpcOut toGrpc(DomainOut domainOut);

    public Multi<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
        try {
            DomainIn domainIn = fromGrpc(grpcRequest);
            Stream<DomainOut> stream = getService().process(domainIn);

            // Wrap the Java Stream as a Mutiny Multi
            return Multi.createFrom().items(stream)
                    .onItem().transform(this::toGrpc)
                    .onTermination().invoke(stream::close); // Ensure stream is closed

        } catch (Exception e) {
            return Multi.createFrom().failure(e);
        }
    }
}
