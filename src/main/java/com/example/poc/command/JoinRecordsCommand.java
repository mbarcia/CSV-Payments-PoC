package com.example.poc.command;

import com.example.poc.biz.CSVFolder;
import com.example.poc.biz.CSVPaymentsFile;
import com.example.poc.biz.PaymentRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;
@Service
public class JoinRecordsCommand extends BaseCommand<CSVFolder, Stream<PaymentRecord>> {
    @Autowired
    private ReadFileCommand readCsvPaymentsFileCommand;

    @Autowired
    private ReadFolderCommand readFolderCommand;

    @Override
    public Stream<PaymentRecord> execute(CSVFolder csvFolder) {
        super.execute(csvFolder);

        Stream<PaymentRecord> results = Stream.empty();

        for (CSVPaymentsFile file : readFolderCommand.execute(csvFolder)) {
            results = Stream.concat(results, readCsvPaymentsFileCommand.execute(file));
        }

        return results;
    }
}
