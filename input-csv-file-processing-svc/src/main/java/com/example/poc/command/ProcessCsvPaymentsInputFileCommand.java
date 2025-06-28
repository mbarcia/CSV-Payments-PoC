package com.example.poc.command;

import com.example.poc.common.command.Command;
import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.stream.Stream;

@ApplicationScoped
public class ProcessCsvPaymentsInputFileCommand implements Command<CsvPaymentsInputFile, Stream<PaymentRecord>> {
    @Override
    public Stream<PaymentRecord> execute(CsvPaymentsInputFile csvFile) {
        Logger logger = LoggerFactory.getLogger(getClass());

        try {
            CsvToBean<PaymentRecord> csvReader = new CsvToBeanBuilder<PaymentRecord>(new BufferedReader(new FileReader(csvFile.getFilepath())))
                    .withType(PaymentRecord.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            return csvReader.parse().stream()
                    .map(record -> record.assignInputFile(csvFile));
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }
}
