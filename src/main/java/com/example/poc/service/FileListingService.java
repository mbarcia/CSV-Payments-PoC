package com.example.poc.service;

import java.io.File;

public interface FileListingService {
    File[] listCsvFiles(String directoryPath);
}
