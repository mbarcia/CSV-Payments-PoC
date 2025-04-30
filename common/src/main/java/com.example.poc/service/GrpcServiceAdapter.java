package com.example.poc.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public abstract class GrpcServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {
    protected abstract Service<DomainIn, DomainOut> getService();
    protected abstract DomainIn fromGrpc(GrpcIn grpcIn);
    protected abstract GrpcOut toGrpc(DomainOut domainOut);

    public void invoke(GrpcIn grpcRequest, StreamObserver<GrpcOut> responseObserver) {
        try {
            DomainIn domainIn = fromGrpc(grpcRequest);
            DomainOut domainOut = getService().process(domainIn); // uses the `process()` method
            GrpcOut grpcResponse = toGrpc(domainOut);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Processing failed: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}