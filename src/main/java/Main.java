import poc.Command;
import poc.csvfolder.CSVFolder;
import poc.csvrecord.JoinRecordsCommand;
import poc.csvrecord.PaymentRecord;
import poc.csvrecord.SendPaymentCommand;

import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        CSVFolder aFolder = new CSVFolder("/Users/mari/IdeaProjects/CSV Payments PoC/src/test/files");
        // Command creation (can be done with IoC, ie. SpringBoot)
        Command<CSVFolder, Stream<PaymentRecord>> readFolderCommand = new JoinRecordsCommand();
        Command<PaymentRecord, PaymentRecord> sendPaymentCommand = new SendPaymentCommand();
        // Command execution
        readFolderCommand.execute(aFolder) // reads, parses and aggregates records from all CSV files in the folder tree
                .map(sendPaymentCommand::execute) // 1-to-1 command dealing with each payment
                .forEach(System.out::println); // for terminating the stream
    }
}
