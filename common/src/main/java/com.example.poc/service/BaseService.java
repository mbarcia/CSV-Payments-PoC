package com.example.poc.service;

import com.example.poc.command.Command;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

@Getter
public abstract class BaseService<T, S> {
    private Command<T, S> command;

    public BaseService() {}

    public BaseService(Command<T, S> command) {
        this.command = command;
    }

    @Transactional
    public S process(T processableObj) {
        // Log for basic audit purposes
        MDC.put("transactionId", UUID.randomUUID().toString());
        MDC.put("serviceId", this.getClass().toString());

        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Executed command {}", processableObj);

        MDC.clear();

        return command.execute(processableObj);
    }
}
