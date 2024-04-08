package com.example.poc.command;

import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.repository.CsvFolderRepository;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

@Component
public class ReadFolderCommand extends BaseCommand<CsvFolder, Stream<CsvPaymentsFile>> {
    private final CsvFolderRepository repository;

    public ReadFolderCommand(CsvFolderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Stream<CsvPaymentsFile> execute(CsvFolder csvFolder) {
        super.execute(csvFolder, repository);

        Stream<File> files = Arrays.stream(getFileList(csvFolder.getFolderPath()));
        ArrayList<CsvPaymentsFile> retFiles = new ArrayList<>();
        files.map(f -> {
            try (CsvPaymentsFile file = new CsvPaymentsFile(f)) {
                return file.setCsvFolder(csvFolder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).forEach(retFiles::add);

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
