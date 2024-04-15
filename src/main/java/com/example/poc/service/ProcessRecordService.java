package com.example.poc.service;

import com.example.poc.command.ProcessRecordCommand;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.PaymentRecordRepository;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.stereotype.Service;

@Service
public class ProcessRecordService extends BaseService<PaymentRecord, PaymentOutput> {
    public ProcessRecordService(PaymentRecordRepository repository, ProcessRecordCommand command) {
        super(repository, command);
    }

    public void writeOutputToFile(PaymentRecord paymentRecord, PaymentOutput paymentOutput) throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException {
        paymentRecord.getCsvPaymentsFile().getSbc().write(paymentOutput);
    }
}
