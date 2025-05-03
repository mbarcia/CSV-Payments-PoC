package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.service.CsvFileService;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ReadFolderCommand implements Command<CsvFolder, Map<CsvPaymentsInputFile, CsvPaymentsOutputFile>> {
    private final CsvFileService csvFileService;

    public ReadFolderCommand(
            CsvFileService csvFileService) {
        this.csvFileService = csvFileService;
    }

    @Override
    public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> execute(CsvFolder csvFolder) {
        File[] files = csvFileService.listCsvFiles(csvFolder.getFolderPath());
        HashMap<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = new HashMap<>();

        for (File file : files) {
            CsvPaymentsInputFile inputFile = csvFileService.createInputCsvFile(file);
            inputFile.setCsvFolder(csvFolder);
            try {
                CsvPaymentsOutputFile outputFile = csvFileService.createOutputCsvFile(inputFile);
                result.put(inputFile, outputFile);
            } catch (IOException ignored) {
            }
        }

        return result;
    }
}
