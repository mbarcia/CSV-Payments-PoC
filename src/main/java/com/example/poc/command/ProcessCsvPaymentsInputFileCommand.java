package com.example.poc.command;

import com.example.poc.domain.*;
import com.example.poc.service.*;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.stream.Stream;

@Component
public class ProcessCsvPaymentsInputFileCommand implements Command<CsvPaymentsInputFile, Stream<PaymentOutput>> {
    private final ProcessAckPaymentSentService processAckPaymentSentService;
    private final ProcessPaymentOutputStreamService processPaymentOutputStreamService;
    private final SendPaymentRecordService sendPaymentRecordService;
    private final BaseService<PaymentStatus, PaymentOutput> processPaymentStatusService;

    public ProcessCsvPaymentsInputFileCommand(ProcessAckPaymentSentService processAckPaymentSentService, ProcessPaymentOutputStreamService processPaymentOutputStreamService, SendPaymentRecordService sendPaymentRecordService, ProcessPaymentStatusService processPaymentStatusService) {
        this.processAckPaymentSentService = processAckPaymentSentService;
        this.processPaymentOutputStreamService = processPaymentOutputStreamService;
        this.sendPaymentRecordService = sendPaymentRecordService;
        this.processPaymentStatusService = processPaymentStatusService;
    }

    @Override
    public Stream<PaymentOutput> execute(CsvPaymentsInputFile csvFile) {
        try (CsvPaymentsOutputFile csvOutputFile = processPaymentOutputStreamService.createCsvFile(csvFile)) {
            CsvToBean<PaymentRecord> csvReader = new CsvToBeanBuilder<PaymentRecord>(new BufferedReader(new FileReader(csvFile.getFilepath())))
                    .withType(PaymentRecord.class)
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            return csvReader.parse().stream()
                    .map(record -> record.setCsvPaymentsInputFile(csvFile))
                    .map(record -> record.setCsvPaymentsOutputFile(csvOutputFile))
                    .map(sendPaymentRecordService::process)
                    // Process records for each file asynchronously
                    .map(processAckPaymentSentService::process)
                    .map(processPaymentStatusService::process);
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }
}
