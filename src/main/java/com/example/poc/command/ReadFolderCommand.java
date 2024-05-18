package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.service.ProcessCsvPaymentsInputFileService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

@Component
public class ReadFolderCommand implements Command<CsvFolder, Stream<CsvPaymentsInputFile>> {
    final
    ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService;

    public ReadFolderCommand(ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService) {
        this.processCsvPaymentsInputFileService = processCsvPaymentsInputFileService;
    }

    @Override
    public Stream<CsvPaymentsInputFile> execute(CsvFolder csvFolder) {
        Stream<File> files = Arrays.stream(getFileList(csvFolder.getFolderPath()));
        ArrayList<CsvPaymentsInputFile> retFiles = new ArrayList<>();
        files.map(processCsvPaymentsInputFileService::createCsvFile).forEach(file -> {
            file.setCsvFolder(csvFolder);
            retFiles.add(file);
        });

        return retFiles.stream();
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
