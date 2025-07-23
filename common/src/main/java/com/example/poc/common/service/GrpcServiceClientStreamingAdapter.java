package com.example.poc.common.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public abstract class GrpcServiceClientStreamingAdapter<GrpcIn, GrpcOut, DomainIn, DomainOut> {

  protected abstract ReactiveStreamingClientService<DomainIn, DomainOut> getService();

  protected abstract DomainIn fromGrpc(GrpcIn grpcIn);

  protected abstract GrpcOut toGrpc(DomainOut domainOut);

  public Uni<GrpcOut> remoteProcess(Multi<GrpcIn> requestStream) {

    return getService()
        .process(
            requestStream.onItem().transform(this::fromGrpc)) // Multi<DomainIn> → Uni<DomainOut>
        .onItem()
        .transform(this::toGrpc) // Uni<GrpcOut>
        .onFailure()
        .transform(new throwStatusRuntimeExceptionFunction());
  }
}
