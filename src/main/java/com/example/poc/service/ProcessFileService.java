package com.example.poc.service;

import com.example.poc.command.ProcessFileCommand;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.repository.CsvPaymentsFileRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class ProcessFileService extends BaseService<CsvPaymentsFile, List<PaymentOutput>> {

    public ProcessFileService(CsvPaymentsFileRepository repository, ProcessFileCommand command) {
        super(repository, command);
    }

    public CsvPaymentsFile createCsvFile(File file) throws IOException {
        return new CsvPaymentsFile(file);
    }
}
