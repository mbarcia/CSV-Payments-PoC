package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.service.ProcessCsvPaymentsInputFileService;
import com.example.poc.service.ProcessPaymentOutputService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ReadFolderCommand implements Command<CsvFolder, Map<CsvPaymentsInputFile, CsvPaymentsOutputFile>> {
    final
    ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService;
    final ProcessPaymentOutputService processPaymentOutputService;

    public ReadFolderCommand(ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService, ProcessPaymentOutputService processPaymentOutputService) {
        this.processCsvPaymentsInputFileService = processCsvPaymentsInputFileService;
        this.processPaymentOutputService = processPaymentOutputService;
    }

    @Override
    public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> execute(CsvFolder csvFolder) {
        File[] files = getFileList(csvFolder.getFolderPath());
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

    /**
     * @param dir String path
     * @return A set of files
     */
    private File[] getFileList(String dir) {
        File directory = new File(dir);

        return directory.listFiles((_, name) -> name.toLowerCase().endsWith(".csv"));
    }
}
