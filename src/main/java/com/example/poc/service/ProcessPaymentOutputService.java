package com.example.poc.service;

import com.example.poc.command.ProcessPaymentOutputCommand;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.repository.CsvPaymentsOutputFileRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ProcessPaymentOutputService extends BaseService<PaymentOutput, CsvPaymentsOutputFile> {
    private final CsvPaymentsOutputFileRepository csvPaymentsOutputFileRepository;
    private Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> csvPaymentsOutputFileMap;

    public ProcessPaymentOutputService(ProcessPaymentOutputCommand command, CsvPaymentsOutputFileRepository csvPaymentsOutputFileRepository) {
        super(command);
        this.csvPaymentsOutputFileRepository = csvPaymentsOutputFileRepository;
    }

    public void initialiseFiles(Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> csvPaymentsOutputFileMap) {
        this.csvPaymentsOutputFileMap = csvPaymentsOutputFileMap;
    }

    @Override
    public CsvPaymentsOutputFile process(PaymentOutput processableObj) {
        PaymentRecord paymentRecord = processableObj.getPaymentRecord();
        CsvPaymentsInputFile csvPaymentsInputFile = paymentRecord.getCsvPaymentsInputFile();
        paymentRecord.setCsvPaymentsOutputFile(csvPaymentsOutputFileMap.get(csvPaymentsInputFile));

        return super.process(processableObj);
    }

    public void print() {
        this.csvPaymentsOutputFileRepository.findAll().forEach(System.out::println);
    }

    public void closeFiles(List<CsvPaymentsOutputFile> outputFilesList) {
        outputFilesList.forEach(
            (outputFile) -> {
                try {
                    outputFile.getWriter().close();
                } catch (IOException e) {
                    // ignore and continue
                }
            }
        );
    }

    public CsvPaymentsOutputFile createCsvFile(CsvPaymentsInputFile inputFile) throws IOException {
        return new CsvPaymentsOutputFile(inputFile);
    }
}
