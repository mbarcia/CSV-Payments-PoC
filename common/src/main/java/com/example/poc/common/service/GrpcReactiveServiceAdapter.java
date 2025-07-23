package com.example.poc.common.service;

import io.smallrye.mutiny.Uni;

public abstract class GrpcReactiveServiceAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  protected abstract ReactiveService<DomainIn, DomainOut> getService();

  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  protected abstract GrpcOut toGrpc(DomainOut domainOut);

  public Uni<GrpcOut> remoteProcess(GrpcIn grpcRequest) {

    return getService()
        .process(fromGrpc(grpcRequest))
        .onItem()
        .transform(this::toGrpc)
        .onFailure()
        .transform(new throwStatusRuntimeExceptionFunction());
  }
}
