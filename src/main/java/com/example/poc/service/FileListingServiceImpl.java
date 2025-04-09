package com.example.poc.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.util.Objects;

@ApplicationScoped
public class FileListingServiceImpl implements FileListingService {
    @Override
    public File[] listCsvFiles(String directoryPath) {
        if (Objects.nonNull(directoryPath)) {
            File directory = new File(directoryPath);

            return directory.listFiles((file, name) -> name.toLowerCase().endsWith(".csv"));
        } else {
            return new File[0];
        }
    }
}
