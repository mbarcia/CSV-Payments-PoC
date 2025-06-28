package com.example.poc.service;

import com.example.poc.common.command.Command;
import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.service.LocalAbstractServiceWithAudit;
import com.example.poc.repository.CsvPaymentsInputFileRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.stream.Stream;

@ApplicationScoped
public class ProcessCsvPaymentsInputFileService extends LocalAbstractServiceWithAudit<CsvPaymentsInputFile, Stream<PaymentRecord>> {
    public ProcessCsvPaymentsInputFileService(Command<CsvPaymentsInputFile, Stream<PaymentRecord>> command, CsvPaymentsInputFileRepository repository) {
        super(repository, command);
    }
}
