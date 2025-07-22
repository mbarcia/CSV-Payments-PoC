package com.example.poc.common.service;

import com.example.poc.common.command.ReactiveCommand;
import com.example.poc.common.domain.BaseEntity;
import io.smallrye.mutiny.Uni;

public abstract class BasePersistedReactiveService<T extends BaseEntity, S> extends BaseReactiveService<T, S> {
    @Override
    public Uni<S> process(T processableObj) {
        return processableObj.save()
            .replaceWith(super.process(processableObj));
    }

    @Override
    public abstract ReactiveCommand<T, S> getCommand();
}
