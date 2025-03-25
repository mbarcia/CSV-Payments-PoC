package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.service.FileListingService;
import com.example.poc.service.ProcessCsvPaymentsInputFileService;
import com.example.poc.service.ProcessPaymentOutputService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ReadFolderCommand implements Command<CsvFolder, Map<CsvPaymentsInputFile, CsvPaymentsOutputFile>> {
    private final ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService;
    private final ProcessPaymentOutputService processPaymentOutputService;
    private final FileListingService fileListingService;

    public ReadFolderCommand(
            ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService,
            ProcessPaymentOutputService processPaymentOutputService,
            FileListingService fileListingService) {
        this.processCsvPaymentsInputFileService = processCsvPaymentsInputFileService;
        this.processPaymentOutputService = processPaymentOutputService;
        this.fileListingService = fileListingService;
    }

    @Override
    public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> execute(CsvFolder csvFolder) {
        File[] files = fileListingService.listCsvFiles(csvFolder.getFolderPath());
        HashMap<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = new HashMap<>();

        for (File file : files) {
            CsvPaymentsInputFile inputFile = processCsvPaymentsInputFileService.createCsvFile(file);
            inputFile.setCsvFolder(csvFolder);
            try {
                CsvPaymentsOutputFile outputFile = processPaymentOutputService.createCsvFile(inputFile);
                result.put(inputFile, outputFile);
            } catch (IOException _) {
            }
        }

        return result;
    }
}
