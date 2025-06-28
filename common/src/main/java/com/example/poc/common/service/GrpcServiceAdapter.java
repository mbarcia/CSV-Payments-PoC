package com.example.poc.common.service;

import io.smallrye.mutiny.Uni;

public abstract class GrpcServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

    protected abstract Service<DomainIn, DomainOut> getService();
    protected abstract DomainIn fromGrpc(GrpcIn grpcIn);
    protected abstract GrpcOut toGrpc(DomainOut domainOut);

    public Uni<GrpcOut> remoteProcess(GrpcIn grpcRequest) {
        return Uni.createFrom().item(() -> {
            DomainIn domainIn = fromGrpc(grpcRequest);
            DomainOut domainOut = getService().process(domainIn);
            return toGrpc(domainOut);
        });
    }
}
