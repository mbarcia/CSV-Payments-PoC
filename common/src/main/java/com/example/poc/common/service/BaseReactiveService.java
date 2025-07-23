package com.example.poc.common.service;

import com.example.poc.common.command.ReactiveCommand;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class BaseReactiveService<T, S> implements ReactiveService<T, S> {

  @Override
  public Uni<S> process(T processableObj) {

    return getCommand()
        .execute(processableObj)
        .invoke(
            result -> {
              String serviceId = this.getClass().toString();
              Logger logger = LoggerFactory.getLogger(this.getClass());
              MDC.put("serviceId", serviceId);
              logger.info("Executed command on {} --> {}", processableObj, result);
              MDC.clear();
            });
  }

  @Override
  public abstract ReactiveCommand<T, S> getCommand();
}
