package com.example.poc.common.command;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface ReactiveStreamingClientCommand<T, S> {
  Uni<S> execute(Multi<T> input);
}
