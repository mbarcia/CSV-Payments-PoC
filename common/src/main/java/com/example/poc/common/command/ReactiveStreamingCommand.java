package com.example.poc.common.command;

import io.smallrye.mutiny.Multi;

@FunctionalInterface
public interface ReactiveStreamingCommand<T, S> {
    Multi<S> execute(T input);
}
