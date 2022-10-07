package poc.csvrecord;

import poc.Command;
import poc.csvfile.CSVPaymentsFile;
import poc.csvfile.ReadFileCommand;
import poc.csvfolder.CSVFolder;
import poc.csvfolder.ReadFolderCommand;

import java.util.Set;
import java.util.stream.Stream;

public class JoinRecordsCommand implements Command<CSVFolder, Stream<PaymentRecord>> {
    @Override
    public Stream<PaymentRecord> execute(CSVFolder csvFolder) {
        // Commands creation (can be done with IoC ie. SpringBoot)
        Command<CSVFolder, Set<CSVPaymentsFile>> readFolderCommand = new ReadFolderCommand();
        Command<CSVPaymentsFile, Stream<PaymentRecord>> readCsvPaymentsFileCommand = new ReadFileCommand();

        Stream<poc.csvrecord.PaymentRecord> results = Stream.empty();

        for (CSVPaymentsFile file : readFolderCommand.execute(csvFolder)) {
            results = Stream.concat(results, readCsvPaymentsFileCommand.execute(file));
        }

        return results;
    }
}
