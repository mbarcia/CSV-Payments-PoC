package com.example.poc.service;

import com.example.poc.command.Command;
import lombok.Getter;
import org.springframework.data.repository.CrudRepository;

@Getter
public abstract class BaseServiceWithAudit<T, S> extends BaseService<T, S> {
    private final CrudRepository<T, Long> repository;

    public BaseServiceWithAudit(CrudRepository<T, Long> repository, Command<T, S> command) {
        super(command);
        this.repository = repository;
    }

    @Override
    public S process(T processableObj) {
        // Save command object to the database for advanced audit purposes
        repository.save(processableObj);
        return super.process(processableObj);
    }

    public void print() {
        repository.findAll().forEach(System.out::println);
    }
}

