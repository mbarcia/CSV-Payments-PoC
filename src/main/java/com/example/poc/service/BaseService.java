package com.example.poc.service;

import com.example.poc.command.Command;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;

@Getter
public abstract class BaseService<T, S> {
    private final CrudRepository<T, Long> repository;
    private final Command<T, S> command;

    public BaseService(CrudRepository<T, Long> repository, Command<T, S> command) {
        this.repository = repository;
        this.command = command;
    }

    public S process(T processableObj) {
        repository.save(processableObj);

        // Log for audit purposes
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Executed with {}", processableObj);

        return command.execute(processableObj);
    }

    public void print() {
        repository.findAll().forEach(System.out::println);
    }
}

