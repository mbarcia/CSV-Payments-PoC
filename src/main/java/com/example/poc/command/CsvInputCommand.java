package com.example.poc.command;

import com.example.poc.biz.CsvPaymentsFile;
import com.example.poc.biz.PaymentRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.stream.Stream;

@Component
public class CsvInputCommand extends BaseCommand<CsvPaymentsFile, Stream<PaymentRecord>> {
    @Autowired
    private ReadFileCommand readFileCommand;
    @Autowired
    private SendPaymentCommand sendPaymentCommand;
    @Autowired
    private PersistRecordCommand persistRecordCommand;

    @Transactional
    public Stream<PaymentRecord> execute(CsvPaymentsFile aFile) {
        super.execute(aFile);

        return readFileCommand.execute(aFile).
                map(sendPaymentCommand::execute).
                map(persistRecordCommand::execute);
        }
}
