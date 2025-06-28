package com.example.poc.common.service;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Dependent
public class LocalServiceTransactionalDelegate<T> {

    @Inject
    public LocalServiceTransactionalDelegate() {
        // CDI will handle creation, so no need to pass the service manually
    }

    @Transactional
    void persist(PanacheRepository<T> repository, T processableObj) {
        // Save command object to the database for advanced audit purposes
        repository.persist(processableObj);
    }
}