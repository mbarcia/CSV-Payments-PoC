package com.example.poc.service;

import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Stream;

@Service
public class ReadFolderService extends BaseService<CsvFolder, Stream<CsvPaymentsFile>> {

    public static final String CSV_FOLDER = "csv/";

    public ReadFolderService(CrudRepository<CsvFolder, Long> repository, ReadFolderCommand command) {
        super(repository, command);
    }

    public Stream<CsvPaymentsFile> process(String[] args) throws URISyntaxException {
        return this.process(getCsvFolder(args));
    }

    private CsvFolder getCsvFolder(String[] args) throws URISyntaxException {
        // Default to CSV_FOLDER if no param has been provided
        String folder = args.length == 0 ? CSV_FOLDER : args[0];
        // Get the file system resource
        URL resource = ReadFolderService.class.getClassLoader().getResource(folder);
        assert resource != null;

        // Return the domain object
        return new CsvFolder(resource.toURI().getPath());
    }
}
