package com.example.poc.command;

import com.example.poc.domain.BasePersistable;
import com.example.poc.repository.BaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseCommand<T extends BasePersistable, S> implements Command<T, S> {
    public S execute(T processableObj, BaseRepository<T> repository) {
        // Firstly, save (or update) the given object
        repository.save(processableObj);

        // Log for audit purposes
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Executed with {}", processableObj);

        // Return value should not be taken into account
        return null;
    }
}
