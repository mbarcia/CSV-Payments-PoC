package com.example.poc.command;

import com.example.poc.Command;
import com.example.poc.biz.CSVPaymentsFile;
import com.example.poc.biz.CSVFolder;
import com.example.poc.biz.PaymentRecord;

import java.util.Set;
import java.util.stream.Stream;

public class JoinRecordsCommand implements Command<CSVFolder, Stream<PaymentRecord>> {
    @Override
    public Stream<PaymentRecord> execute(CSVFolder csvFolder) {
        // Commands creation (can be done with IoC i.e. SpringBoot)
        Command<CSVFolder, Set<CSVPaymentsFile>> readFolderCommand = new ReadFolderCommand();
        Command<CSVPaymentsFile, Stream<PaymentRecord>> readCsvPaymentsFileCommand = new ReadFileCommand();

        Stream<PaymentRecord> results = Stream.empty();

        for (CSVPaymentsFile file : readFolderCommand.execute(csvFolder)) {
            results = Stream.concat(results, readCsvPaymentsFileCommand.execute(file));
        }

        return results;
    }
}
