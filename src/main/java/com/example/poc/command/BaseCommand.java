package com.example.poc.command;

import com.example.poc.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseCommand<T, S> implements Command<T, S> {
    public S execute(T processableObj) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Executing with {}", processableObj.toString());
        return null;
    }
}
