package com.example.poc.client;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

@QuarkusMain
public class CsvPaymentsApplication implements QuarkusApplication {

    @Inject
    @TopCommand
    CsvCommand csvCommand;

    @Override
    public int run(String... args) {
        return new CommandLine(csvCommand).execute(args);
    }
}
