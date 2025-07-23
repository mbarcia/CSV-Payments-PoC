package com.example.poc.common.service;

import com.example.poc.common.command.ReactiveStreamingCommand;
import io.smallrye.mutiny.Multi;

public interface ReactiveStreamingService<T, S> {
  Multi<S> process(T processableObj);

  ReactiveStreamingCommand<T, S> getCommand();
}
