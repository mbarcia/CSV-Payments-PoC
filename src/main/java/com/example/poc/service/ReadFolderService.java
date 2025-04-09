package com.example.poc.service;

import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.repository.CsvFolderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class ReadFolderService extends BaseServiceWithAudit<CsvFolder, Map<CsvPaymentsInputFile, CsvPaymentsOutputFile>> {

    public static final String CSV_FOLDER = "csv/";

    private final ResourceLoader resourceLoader;

    @Inject
    public ReadFolderService(CsvFolderRepository repository, ReadFolderCommand command, ResourceLoader resourceLoader) {
        super(repository, command);
        this.resourceLoader = resourceLoader;
    }

    @Transactional
    public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> process(String csvFolder) {
        try {
            return super.process(getCsvFolder(csvFolder));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private CsvFolder getCsvFolder(String csvFolder) throws URISyntaxException, IllegalArgumentException {
        // Default to CSV_FOLDER if no param has been provided
        String folder = Objects.isNull(csvFolder) || csvFolder.isEmpty() ? CSV_FOLDER : csvFolder;
        URL resource = resourceLoader.getResource(folder);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + folder);
        }

        return new CsvFolder(resource.toURI().getPath());
    }
}
