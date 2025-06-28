package com.example.poc.common.service;

import com.example.poc.common.command.Command;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.inject.Inject;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class LocalAbstractServiceWithAudit<T, S> extends LocalAbstractService<T, S> implements Service<T, S> {

    @Inject
    LocalServiceTransactionalDelegate<T> localServiceTransactionalDelegate;

    private PanacheRepository<T> repository;

    @SuppressWarnings("unused")
    public LocalAbstractServiceWithAudit() {}

    public LocalAbstractServiceWithAudit(PanacheRepository<T> repository, Command<T, S> command) {
        super(command);
        this.repository = repository;
    }

    @Override
    public S process(T processableObj) {
        System.out.println("Persisting entity: " + processableObj.toString());
        this.persist(processableObj);
        return super.process(processableObj);
    }

    protected void persist(T processableObj) {
        // Save command object to the database for advanced audit purposes
        localServiceTransactionalDelegate.persist(getRepository(), processableObj);
    }

    public void print() {
        List<T> entities = repository.listAll();
        entities.forEach(System.out::println);
    }
}
