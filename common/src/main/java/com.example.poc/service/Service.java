package com.example.poc.service;

import jakarta.transaction.Transactional;

public interface Service<T, S> {
    @Transactional
    S process(T processableObj);

    com.example.poc.command.Command<T, S> getCommand();
}
