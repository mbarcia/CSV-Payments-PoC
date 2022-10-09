package com.example.poc.command;

public interface Command<T, S> {
    S execute(T processableObj);
}
