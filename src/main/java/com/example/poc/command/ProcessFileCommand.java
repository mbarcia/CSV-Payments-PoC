package com.example.poc.command;

import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.repository.CsvPaymentsFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessFileCommand extends BaseCommand<CsvPaymentsFile, List<PaymentOutput>> {
    private final ProcessRecordCommand processRecordCommand;

    private final CsvPaymentsFileRepository repository;

    public ProcessFileCommand(ProcessRecordCommand processRecordCommand, CsvPaymentsFileRepository repository) {
        this.processRecordCommand = processRecordCommand;
        this.repository = repository;
    }

    @Override
    public List<PaymentOutput> execute(CsvPaymentsFile csvFile) {
        super.execute(csvFile, repository);
        try {
            return csvFile.getCsvReader().parse().stream()
                    .map(record -> record.setCsvPaymentsFile(csvFile))
                    // Process records for each file asynchronously
                    .map(processRecordCommand::execute).toList();
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.error(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }
}
