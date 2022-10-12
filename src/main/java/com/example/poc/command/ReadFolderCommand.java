package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.repository.CsvFolderRepository;
import com.example.poc.repository.CsvPaymentsFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class ReadFolderCommand extends BaseCommand<CsvFolder, Set<CsvPaymentsFile>> {
    @Autowired
    private CsvPaymentsFileRepository csvPaymentsFileRepository;

    @Autowired
    private CsvFolderRepository csvFolderRepository;

    @Transactional
    @Override
    public Set<CsvPaymentsFile> execute(CsvFolder csvFolder) {
        super.execute(csvFolder);

        csvFolderRepository.save(csvFolder);
        Set<CsvPaymentsFile> fileSet = new HashSet<>();
        for (File result : Objects.requireNonNull(getFileList(csvFolder.toString()))) {
            CsvPaymentsFile csvPaymentsFile = new CsvPaymentsFile(result);
            csvPaymentsFile.setCsvFolder(csvFolder);
            csvPaymentsFileRepository.save(csvPaymentsFile);
            fileSet.add(csvPaymentsFile);
        }

        return fileSet;
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
