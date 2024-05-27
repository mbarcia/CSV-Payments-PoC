package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.PaymentRecord;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.stream.Stream;

@Component
public class ProcessCsvPaymentsInputFileCommand implements Command<CsvPaymentsInputFile, Stream<PaymentRecord>> {
    @Override
    public Stream<PaymentRecord> execute(CsvPaymentsInputFile csvFile) {
        try {
            CsvToBean<PaymentRecord> csvReader = new CsvToBeanBuilder<PaymentRecord>(new BufferedReader(new FileReader(csvFile.getFilepath())))
                    .withType(PaymentRecord.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            return csvReader.parse().stream()
                    .map(record -> record.setCsvPaymentsInputFile(csvFile));
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }
}
