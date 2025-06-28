package com.example.poc.common.service;

import com.example.poc.common.command.Command;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

@Getter
public abstract class LocalAbstractService<T, S> implements Service<T, S> {
    private Command<T, S> command;

    public LocalAbstractService() {}

    public LocalAbstractService(Command<T, S> command) {
        this.command = command;
    }

    @Override
    public S process(T processableObj) {
        // Log for basic audit purposes
        MDC.put("transactionId", UUID.randomUUID().toString());
        MDC.put("serviceId", this.getClass().toString());

        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Executed command {}", processableObj);

        MDC.clear();

        return getCommand().execute(processableObj);
    }
}
