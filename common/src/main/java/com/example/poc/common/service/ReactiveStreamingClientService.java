package com.example.poc.common.service;

import com.example.poc.common.command.ReactiveStreamingClientCommand;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface ReactiveStreamingClientService<T, S> {
  Uni<S> process(Multi<T> processableObj);

  ReactiveStreamingClientCommand<T, S> getCommand();
}
