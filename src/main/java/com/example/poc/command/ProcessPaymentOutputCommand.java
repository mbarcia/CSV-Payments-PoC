package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentOutput;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.stereotype.Component;

@Component
public class ProcessPaymentOutputCommand implements Command<PaymentOutput, CsvPaymentsOutputFile> {
    @Override
    public CsvPaymentsOutputFile execute(PaymentOutput paymentOutput) {
        try {
            CsvPaymentsOutputFile csvOutputFile = paymentOutput.getPaymentRecord().getCsvPaymentsOutputFile();
            csvOutputFile.getSbc().write(paymentOutput);

            return csvOutputFile;
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            throw new RuntimeException(e);
        }
    }
}
