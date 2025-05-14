package com.example.poc.service;

public interface Service<T, S> {
    S process(T processableObj);

    com.example.poc.command.Command<T, S> getCommand();
}
