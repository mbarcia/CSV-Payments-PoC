package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.service.ProcessRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessFileCommand implements Command<CsvPaymentsFile, List<PaymentOutput>> {
    private final ProcessRecordService processRecordService;

    public ProcessFileCommand(ProcessRecordService processRecordService) {
        this.processRecordService = processRecordService;
    }

    @Override
    public List<PaymentOutput> execute(CsvPaymentsFile csvFile) {
        try {
            List<PaymentOutput> list = csvFile.getCsvReader().parse().stream()
                    .map(record -> record.setCsvPaymentsFile(csvFile))
                    // Process records for each file asynchronously
                    .map(processRecordService::process).toList();
            csvFile.close();
            return list;
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }
}
