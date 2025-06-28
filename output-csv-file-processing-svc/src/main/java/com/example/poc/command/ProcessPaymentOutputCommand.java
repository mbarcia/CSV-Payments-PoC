package com.example.poc.command;

import com.example.poc.common.command.Command;
import com.example.poc.common.domain.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class ProcessPaymentOutputCommand implements Command<List<PaymentOutput>, CsvPaymentsOutputFile> {
    @Override
    public CsvPaymentsOutputFile execute(List<PaymentOutput> paymentOutputList) {
        try {
            CsvPaymentsOutputFile csvOutputFile = getCsvPaymentsOutputFile(paymentOutputList.getFirst());
            csvOutputFile.getSbc().write(paymentOutputList);
            csvOutputFile.getWriter().close();

            return csvOutputFile;
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CsvPaymentsOutputFile getCsvPaymentsOutputFile(PaymentOutput paymentOutput) throws IOException {
        assert paymentOutput != null;
        PaymentStatus paymentStatus = paymentOutput.getPaymentStatus();
        AckPaymentSent ackPaymentSent = paymentStatus.getAckPaymentSent();
        PaymentRecord paymentRecord = ackPaymentSent.getPaymentRecord();
        String csvPaymentsInputFilePath = paymentRecord.getCsvPaymentsInputFilePath();

        return new CsvPaymentsOutputFile(csvPaymentsInputFilePath);
    }
}
