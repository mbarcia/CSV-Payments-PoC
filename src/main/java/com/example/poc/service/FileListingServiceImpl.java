package com.example.poc.service;

import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class FileListingServiceImpl implements FileListingService {
    @Override
    public File[] listCsvFiles(String directoryPath) {
        File directory = new File(directoryPath);
        return directory.listFiles((file, name) -> name.toLowerCase().endsWith(".csv"));
    }
}
