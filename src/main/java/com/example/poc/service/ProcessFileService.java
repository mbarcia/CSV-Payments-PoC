package com.example.poc.service;

import com.example.poc.command.ProcessFileCommand;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentOutput;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessFileService extends BaseService<CsvPaymentsFile, List<PaymentOutput>> {

    public ProcessFileService(CrudRepository<CsvPaymentsFile, Long> repository, ProcessFileCommand command) {
        super(repository, command);
    }
}
