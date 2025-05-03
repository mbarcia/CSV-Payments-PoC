package com.example.poc.service;

import com.example.poc.command.ProcessCsvPaymentsInputFileCommand;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.CsvPaymentsInputFileRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.stream.Stream;

@ApplicationScoped
public class ProcessCsvPaymentsInputFileService extends LocalAbstractServiceWithAudit<CsvPaymentsInputFile, Stream<PaymentRecord>> {
    public ProcessCsvPaymentsInputFileService(CsvPaymentsInputFileRepository repository, ProcessCsvPaymentsInputFileCommand command) {
        super(repository, command);
    }
}
