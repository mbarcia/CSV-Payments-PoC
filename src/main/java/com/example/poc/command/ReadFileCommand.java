package com.example.poc.command;

import com.example.poc.biz.CSVPaymentsFile;
import com.example.poc.biz.PaymentRecord;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.stream.Stream;

@Service
public class ReadFileCommand extends BaseCommand<CSVPaymentsFile, Stream<PaymentRecord>> {
    @Override
    public Stream<PaymentRecord> execute(CSVPaymentsFile csvFile) {
        super.execute(csvFile);

        try {
            Reader reader = new BufferedReader(new FileReader(csvFile.getFilepath()));

            CsvToBean<PaymentRecord> csvReader = new CsvToBeanBuilder<PaymentRecord>(reader)
                    .withType(PaymentRecord.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            return csvReader.parse().stream().map(record -> record.setFilepath(csvFile));

        } catch (FileNotFoundException ex) {
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.error(ex.getLocalizedMessage());
        }

        return null;
    }
}
