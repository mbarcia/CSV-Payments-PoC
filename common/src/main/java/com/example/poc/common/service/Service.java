package com.example.poc.common.service;

import com.example.poc.common.command.Command;

public interface Service<T, S> {
    S process(T processableObj);

    Command<T, S> getCommand();
}
