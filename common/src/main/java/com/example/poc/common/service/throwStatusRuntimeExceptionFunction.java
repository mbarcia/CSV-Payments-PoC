package com.example.poc.common.service;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.function.Function;

class throwStatusRuntimeExceptionFunction implements Function<Throwable, Throwable> {
  @Override
  public Throwable apply(Throwable throwable) {
    Metadata metadata = new Metadata();
    Metadata.Key<String> errorKey =
        Metadata.Key.of("error-details", Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(errorKey, throwable.getMessage());

    Status status = Status.INTERNAL.withDescription(throwable.getMessage()).withCause(throwable);

    return new StatusRuntimeException(status, metadata);
  }
}
