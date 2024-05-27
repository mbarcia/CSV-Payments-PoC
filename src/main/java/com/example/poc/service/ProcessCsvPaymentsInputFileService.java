package com.example.poc.service;

import com.example.poc.command.ProcessCsvPaymentsInputFileCommand;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.CsvPaymentsInputFileRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.stream.Stream;

@Service
public class ProcessCsvPaymentsInputFileService extends BaseServiceWithAudit<CsvPaymentsInputFile, Stream<PaymentRecord>> {
    public ProcessCsvPaymentsInputFileService(CsvPaymentsInputFileRepository repository, ProcessCsvPaymentsInputFileCommand command) {
        super(repository, command);
    }

    public CsvPaymentsInputFile createCsvFile(File file) {
        return new CsvPaymentsInputFile(file);
    }
}
