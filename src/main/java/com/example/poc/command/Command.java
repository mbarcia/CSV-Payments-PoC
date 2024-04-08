package com.example.poc.command;

public interface Command<Persistable, S> {
    S execute(Persistable processableObj);
}
