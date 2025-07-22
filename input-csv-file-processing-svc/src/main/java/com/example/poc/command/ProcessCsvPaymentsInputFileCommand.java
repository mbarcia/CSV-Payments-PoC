package com.example.poc.command;

import com.example.poc.common.command.ReactiveStreamingCommand;
import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.Executor;

@ApplicationScoped
public class ProcessCsvPaymentsInputFileCommand implements ReactiveStreamingCommand<CsvPaymentsInputFile, PaymentRecord> {

    private Executor executor;

    @Inject
    public ProcessCsvPaymentsInputFileCommand(@Named("virtualExecutor") Executor executor) {
        this.executor = executor;
    }

    @Override
    public Multi<PaymentRecord> execute(CsvPaymentsInputFile csvFile) {
        Logger logger = LoggerFactory.getLogger(getClass());

        try {
            CsvToBean<PaymentRecord> csvReader = new CsvToBeanBuilder<PaymentRecord>(new BufferedReader(new FileReader(csvFile.getFilepath())))
                .withType(PaymentRecord.class)
                .withSeparator(',')
                .withIgnoreLeadingWhiteSpace(true)
                .withIgnoreEmptyLine(true)
                .build();

            return Multi.createFrom().items(() ->
                csvReader.parse().stream()
                    .map(record -> record.assignInputFile(csvFile))
            ).runSubscriptionOn(executor);

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    // for test use
    void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
