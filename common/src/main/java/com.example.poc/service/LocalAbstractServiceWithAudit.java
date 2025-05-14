package com.example.poc.service;

import com.example.poc.command.Command;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class LocalAbstractServiceWithAudit<T, S> extends LocalAbstractService<T, S> implements Service<T, S> {
    private PanacheRepository<T> repository;

    @SuppressWarnings("unused")
    public LocalAbstractServiceWithAudit() {}

    public LocalAbstractServiceWithAudit(PanacheRepository<T> repository, Command<T, S> command) {
        super(command);
        this.repository = repository;
    }

    @Override
    public S process(T processableObj) {
        // Save command object to the database for advanced audit purposes
        repository.persist(processableObj);
        return super.process(processableObj);
    }

    public void print() {
        List<T> entities = repository.listAll();
        entities.forEach(System.out::println);
    }
}
