package com.example.poc;

import com.example.poc.biz.CSVFolder;
import com.example.poc.command.JoinRecordsCommand;
import com.example.poc.biz.PaymentRecord;
import com.example.poc.command.SendPaymentCommand;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.stream.Stream;

@SpringBootApplication
public class CsvPaymentsProofOfConceptApplication {
    public static void main(String[] args) {
        CSVFolder aFolder = new CSVFolder("/Users/mari/IdeaProjects/CSV Payments PoC/src/test/files");
        // Command creation (can be done with IoC, i.e. SpringBoot)
        Command<CSVFolder, Stream<PaymentRecord>> readFolderAndJoinRecordsCommand = new JoinRecordsCommand();
        Command<PaymentRecord, PaymentRecord> sendPaymentCommand = new SendPaymentCommand();
        // Command execution
        readFolderAndJoinRecordsCommand.execute(aFolder).
                map(sendPaymentCommand::execute).
                forEach(System.out::println);
    }
}
