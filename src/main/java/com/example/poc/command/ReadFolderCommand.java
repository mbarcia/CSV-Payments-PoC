package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.service.CsvPaymentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
public class ReadFolderCommand extends BaseCommand<CsvFolder, List<CsvPaymentsFile>> {
    @Autowired
    private CsvPaymentsService csvPaymentsService;

    public ReadFolderCommand(CsvPaymentsService csvPaymentsService) {
        this.csvPaymentsService = csvPaymentsService;
    }

    @Transactional
    @Override
    public List<CsvPaymentsFile> execute(CsvFolder csvFolder) {
        super.execute(csvFolder);

        csvPaymentsService.persistFolder(csvFolder);

        return Arrays.stream(getFileList(csvFolder.toString())).
                map(csvFile -> csvPaymentsService.persistFile(new CsvPaymentsFile(csvFile).setCsvFolder(csvFolder))).
                toList();
    }

    /**
     * @param dir String path
     * @return A set of files
     */
    private File[] getFileList(String dir) {
        File directory = new File(dir);

        return directory.listFiles((dir1, name) -> name.toLowerCase().endsWith(".csv"));
    }
}
