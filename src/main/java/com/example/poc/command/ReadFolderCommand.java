package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.repository.CsvFolderRepository;
import com.example.poc.repository.CsvPaymentsFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.File;
import java.util.*;

@Component
public class ReadFolderCommand extends BaseCommand<CsvFolder, List<CsvPaymentsFile>> {
    @Autowired
    private CsvPaymentsFileRepository csvPaymentsFileRepository;

    @Autowired
    private CsvFolderRepository csvFolderRepository;

    @Transactional
    @Override
    public List<CsvPaymentsFile> execute(CsvFolder csvFolder) {
        super.execute(csvFolder);

        csvFolderRepository.save(csvFolder);

        return Arrays.stream(getFileList(csvFolder.toString())).
                map(csvFile -> csvPaymentsFileRepository.save(new CsvPaymentsFile(csvFile).setCsvFolder(csvFolder))).
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
