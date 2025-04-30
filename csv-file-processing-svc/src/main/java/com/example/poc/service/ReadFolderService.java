package com.example.poc.service;

import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.repository.CsvFolderRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class ReadFolderService extends LocalAbstractServiceWithAudit<CsvFolder, Map<CsvPaymentsInputFile, CsvPaymentsOutputFile>> {
    public ReadFolderService(CsvFolderRepository repository, ReadFolderCommand command) {
        super(repository, command);
    }
}
