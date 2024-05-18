package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentOutput;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class ProcessPaymentOutputStreamCommand implements Command<Stream<PaymentOutput>, List<CsvPaymentsOutputFile>> {
    @Override
    public List<CsvPaymentsOutputFile> execute(Stream<PaymentOutput> paymentOutputStream) {
        List<CsvPaymentsOutputFile> retList = new ArrayList<>();
        paymentOutputStream.forEach(
            paymentOutput -> {
                try {
                    paymentOutput.getCsvPaymentsOutputFile().getSbc().write(paymentOutput);
                    retList.add(paymentOutput.getCsvPaymentsOutputFile());
                } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
                    throw new RuntimeException(e);
                }
            }
        );

        return retList;
    }
}
