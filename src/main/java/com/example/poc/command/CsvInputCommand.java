package com.example.poc.command;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.CsvPaymentsFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.stream.Stream;

@Component
public class CsvInputCommand extends BaseCommand<CsvPaymentsFile, Stream<AckPaymentSent>> {
    @Autowired
    private ReadFileCommand readFileCommand;
    @Autowired
    private SendPaymentCommand sendPaymentCommand;
    @Autowired
    private PersistRecordCommand persistRecordCommand;

    @Transactional
    public Stream<AckPaymentSent> execute(CsvPaymentsFile aFile) {
        super.execute(aFile);

        return readFileCommand.execute(aFile).
                map(persistRecordCommand::execute).
                map(sendPaymentCommand::execute);
        }
}
