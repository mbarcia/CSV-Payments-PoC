package com.example.poc.common.service;

import com.example.poc.common.command.ReactiveCommand;
import io.smallrye.mutiny.Uni;

public interface ReactiveService<T, S> {
    Uni<S> process(T processableObj);

    ReactiveCommand<T, S> getCommand();
}
