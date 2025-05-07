package com.example.poc.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.stream.Stream;

public abstract class GrpcServiceStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

    protected abstract Service<DomainIn, Stream<DomainOut>> getService();
    protected abstract DomainIn fromGrpc(GrpcIn grpcIn);
    protected abstract GrpcOut toGrpc(DomainOut domainOut);

    // --- Streaming (lazy) ---
    public void remoteProcess(GrpcIn grpcRequest, StreamObserver<GrpcOut> responseObserver) {
        try {
            DomainIn domainIn = fromGrpc(grpcRequest);
            try (Stream<DomainOut> results = processStream(domainIn)) {
                results.forEach(domainOut -> {
                    GrpcOut grpcResponse = toGrpc(domainOut);
                    responseObserver.onNext(grpcResponse);
                });
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleError(e, responseObserver);
        }
    }

    // Streaming implementation can be overridden
    protected Stream<DomainOut> processStream(DomainIn domainIn) {
        return getService().process(domainIn); //
    }

    private void handleError(Exception e, StreamObserver<?> observer) {
        observer.onError(Status.INTERNAL
                .withDescription("Processing failed: " + e.getMessage())
                .withCause(e)
                .asRuntimeException());
    }
}
