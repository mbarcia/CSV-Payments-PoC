package com.example.poc.service;

import com.example.poc.command.Command;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public abstract class BaseService<T, S> {
    private final Command<T, S> command;

    public BaseService(Command<T, S> command) {
        this.command = command;
    }

    public S process(T processableObj) {
        // Log for basic audit purposes
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Executed command {}", processableObj);

        return command.execute(processableObj);
    }
}

