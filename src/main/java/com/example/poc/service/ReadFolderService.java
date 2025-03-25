package com.example.poc.service;

import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.repository.CsvFolderRepository;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

@Service
public class ReadFolderService extends BaseServiceWithAudit<CsvFolder, Map<CsvPaymentsInputFile, CsvPaymentsOutputFile>> {

    public static final String CSV_FOLDER = "csv/";

    private final ResourceLoader resourceLoader;

    public ReadFolderService(CsvFolderRepository repository, ReadFolderCommand command, ResourceLoader resourceLoader) {
        super(repository, command);
        this.resourceLoader = resourceLoader;
    }

    public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> process(String[] args) {
        try {
            return super.process(getCsvFolder(args));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private CsvFolder getCsvFolder(String[] args) throws URISyntaxException, IllegalArgumentException {
        // Default to CSV_FOLDER if no param has been provided
        String folder = args.length == 0 ? CSV_FOLDER : args[0];
        URL resource = resourceLoader.getResource(folder);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + folder);
        }

        return new CsvFolder(resource.toURI().getPath());
    }
}
