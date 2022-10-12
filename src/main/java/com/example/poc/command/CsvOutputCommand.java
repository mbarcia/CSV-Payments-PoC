package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Set;

@Component
public class CsvOutputCommand extends BaseCommand<CsvPaymentsFile, CsvPaymentsFile> {
    @Transactional
    public CsvPaymentsFile execute(CsvPaymentsFile aFile) {
        super.execute(aFile);

        Set<PaymentRecord> processedFileData = aFile.getRecords();

        try (Writer writer = new FileWriter(aFile.getFilepath() + ".out")) {

            StatefulBeanToCsv<PaymentRecord> sbc = new StatefulBeanToCsvBuilder<PaymentRecord>(writer)
                    .withQuotechar('\'')
                    .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                    .build();

            ArrayList<PaymentRecord> paymentRecords = new ArrayList<>(processedFileData);
            sbc.write(paymentRecords);
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
            throw new RuntimeException(e);
        }

        return aFile;
    }
}
