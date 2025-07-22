package com.example.poc.common.service;

import com.example.poc.common.command.ReactiveStreamingCommand;
import io.smallrye.mutiny.Multi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class BaseReactiveStreamingService<T,S> implements ReactiveStreamingService<T, S> {
    public Multi<S> process(T processableObj) {
        String serviceId = this.getClass().toString();
        Logger logger = LoggerFactory.getLogger(this.getClass());

        return getCommand().execute(processableObj)
                .invoke(result -> {
                    MDC.put("serviceId", serviceId);
                    logger.info("Executed command on {} --> {}", processableObj, result);
                    MDC.clear();
                });
    }

    @Override
    public abstract ReactiveStreamingCommand<T, S> getCommand();
}
