package com.example.poc;

public interface Command<T, S> {
    S execute(T processableObj);
}
