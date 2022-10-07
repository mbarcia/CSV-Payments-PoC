package com.example.poc;

import com.example.poc.biz.CSVFolder;
import com.example.poc.command.JoinRecordsCommand;
import com.example.poc.command.PersistRecordCommand;
import com.example.poc.command.SendPaymentCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class CsvPaymentsProofOfConceptApplication {
    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(CsvPaymentsProofOfConceptApplication.class, args);
        CSVReader service = applicationContext.getBean(CSVReader.class);
        CSVFolder aFolder = new CSVFolder("/Users/mari/IdeaProjects/CSV Payments PoC/src/test/files");
        service.execute(aFolder);
    }
}

@Service
class CSVReader {
    @Autowired
    private JoinRecordsCommand readFolderAndJoinRecordsCommand;
    @Autowired
    private SendPaymentCommand sendPaymentCommand;
    @Autowired
    private PersistRecordCommand persistRecordCommand;

    public void execute(CSVFolder aFolder) {
        // Command execution
        readFolderAndJoinRecordsCommand.execute(aFolder).
                map(sendPaymentCommand::execute).
                map(persistRecordCommand::execute).
                forEach(System.out::println);
    }
}