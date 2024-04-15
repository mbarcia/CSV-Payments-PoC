package com.example.poc.command;

/**
 * @param <T> Input type for the execute method
 * @param <S> Output type for the execute method
 */
public interface Command<T, S> {
    S execute(T processableObj);
}
