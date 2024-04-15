package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.service.ProcessFileService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

@Component
public class ReadFolderCommand implements Command<CsvFolder, Stream<CsvPaymentsFile>> {
    final
    ProcessFileService processFileService;

    public ReadFolderCommand(ProcessFileService processFileService) {
        this.processFileService = processFileService;
    }

    @Override
    public Stream<CsvPaymentsFile> execute(CsvFolder csvFolder) {
        Stream<File> files = Arrays.stream(getFileList(csvFolder.getFolderPath()));
        ArrayList<CsvPaymentsFile> retFiles = new ArrayList<>();
        for (File f : files.toList()) {
            try (CsvPaymentsFile file = processFileService.createCsvFile(f)) {
                file.setCsvFolder(csvFolder);
                retFiles.add(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

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
