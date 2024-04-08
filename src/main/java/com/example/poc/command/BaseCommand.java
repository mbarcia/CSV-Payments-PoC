package com.example.poc.command;

import com.example.poc.service.CsvPaymentsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseCommand<T, S> implements Command<T, S> {
    @Autowired
    CsvPaymentsService csvPaymentsService;

    public S execute(T processableObj) {
        // Firstly, save (or update) the given object
        persist(processableObj);

        // Log for audit purposes
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Executed with {}", processableObj);

        // Return value should not be taken into account
        return null;
    }

    /**
     * Persist is implemented by subclasses by calling the service
     *
     * @param processableObj Given object
     * @return Same object only now it should be managed by the ORM
     */
    protected abstract T persist(T processableObj);
}
