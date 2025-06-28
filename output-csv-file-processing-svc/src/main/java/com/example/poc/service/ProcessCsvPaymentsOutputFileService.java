package com.example.poc.service;

import com.example.poc.common.command.Command;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.service.LocalAbstractService;
import com.example.poc.repository.CsvPaymentsOutputFileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ProcessCsvPaymentsOutputFileService extends LocalAbstractService<List<PaymentOutput>, CsvPaymentsOutputFile> {
    private final CsvPaymentsOutputFileRepository csvPaymentsOutputFileRepository;

    @Inject
    public ProcessCsvPaymentsOutputFileService(Command<List<PaymentOutput>, CsvPaymentsOutputFile> command, CsvPaymentsOutputFileRepository csvPaymentsOutputFileRepository) {
        super(command);
        this.csvPaymentsOutputFileRepository = csvPaymentsOutputFileRepository;
    }

    @Override
    public CsvPaymentsOutputFile process(List<PaymentOutput> processableObj) {
        return super.process(processableObj);
    }

    public void print() {
        List<CsvPaymentsOutputFile> entities = this.csvPaymentsOutputFileRepository.listAll();
        entities.forEach(System.out::println);
    }
}
