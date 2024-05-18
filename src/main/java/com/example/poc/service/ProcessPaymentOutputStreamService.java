package com.example.poc.service;

import com.example.poc.command.ProcessPaymentOutputStreamCommand;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.repository.CsvPaymentsOutputFileRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ProcessPaymentOutputStreamService extends BaseService<Stream<PaymentOutput>, List<CsvPaymentsOutputFile>> {
    private final CsvPaymentsOutputFileRepository csvPaymentsOutputFileRepository;

    public ProcessPaymentOutputStreamService(ProcessPaymentOutputStreamCommand command, CsvPaymentsOutputFileRepository csvPaymentsOutputFileRepository) {
        super(command);
        this.csvPaymentsOutputFileRepository = csvPaymentsOutputFileRepository;
    }

    public CsvPaymentsOutputFile createCsvFile(CsvPaymentsInputFile csvPaymentsInputFile) throws IOException {
        CsvPaymentsOutputFile csvPaymentsOutputFile = new CsvPaymentsOutputFile(csvPaymentsInputFile);
        // There is no service for Output files, so it needs to be saved upon creation before a payment record starts referencing it
        this.csvPaymentsOutputFileRepository.save(csvPaymentsOutputFile);

        return csvPaymentsOutputFile;
    }

    public void print() {
        this.csvPaymentsOutputFileRepository.findAll().forEach(System.out::println);
    }
}
