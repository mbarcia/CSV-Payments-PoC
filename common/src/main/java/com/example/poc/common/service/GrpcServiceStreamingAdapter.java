package com.example.poc.common.service;

import io.smallrye.mutiny.Multi;

public abstract class GrpcServiceStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  protected abstract ReactiveStreamingService<DomainIn, DomainOut> getService();

  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  protected abstract GrpcOut toGrpc(DomainOut domainOut);

  public Multi<GrpcOut> remoteProcess(GrpcIn grpcRequest) {

    return getService()
        .process(fromGrpc(grpcRequest)) // Multi<DomainOut>
        .onItem()
        .transform(this::toGrpc) // Multi<GrpcOut>
        .onFailure()
        .transform(new throwStatusRuntimeExceptionFunction());
  }
}
