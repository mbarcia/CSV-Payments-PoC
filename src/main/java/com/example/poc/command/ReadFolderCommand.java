package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

@Component
public class ReadFolderCommand extends BaseCommand<CsvFolder, Stream<CsvPaymentsFile>> {
    @Override
    public Stream<CsvPaymentsFile> execute(CsvFolder csvFolder) {
        super.execute(csvFolder);

        return Arrays.stream(getFileList(csvFolder.getFolderPath()))
                .map(CsvPaymentsFile::new)
                .map(file -> file.setCsvFolder(csvFolder));
    }

    @Override
    protected CsvFolder persist(CsvFolder processableObj) {
        return csvPaymentsService.persist(processableObj);
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
