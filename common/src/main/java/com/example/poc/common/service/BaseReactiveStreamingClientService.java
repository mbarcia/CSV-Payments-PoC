package com.example.poc.common.service;

import com.example.poc.common.command.ReactiveStreamingClientCommand;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class BaseReactiveStreamingClientService<T, S> implements ReactiveStreamingClientService<T, S> {

    public Uni<S> process(Multi<T> processableObj) {
        String serviceId = this.getClass().toString();
        Logger logger = LoggerFactory.getLogger(this.getClass());

        return getCommand().execute(processableObj)
                .invoke(result -> {
                    MDC.put("serviceId", serviceId);
                    logger.info("{}: executed command on multiple inputs", result);
                    MDC.clear();
                });
    }

    @Override
    public abstract ReactiveStreamingClientCommand<T, S> getCommand();
}
