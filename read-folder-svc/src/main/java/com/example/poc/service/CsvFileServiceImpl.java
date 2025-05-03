package com.example.poc.service;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@ApplicationScoped
public class CsvFileServiceImpl implements CsvFileService {
    @Override
    public File[] listCsvFiles(String directoryPath) {
        if (Objects.nonNull(directoryPath)) {
            File directory = new File(directoryPath);

            return directory.listFiles((file, name) -> name.toLowerCase().endsWith(".csv"));
        } else {
            return new File[0];
        }
    }

    @Override
    public CsvPaymentsInputFile createInputCsvFile(File file) {
        return new CsvPaymentsInputFile(file);
    }

    @Override
    public CsvPaymentsOutputFile createOutputCsvFile(CsvPaymentsInputFile inputFile) throws IOException {
        return new CsvPaymentsOutputFile(inputFile);
    }
}
