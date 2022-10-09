package com.example.poc;

import com.example.poc.command.CsvInputCommand;
import com.example.poc.command.CsvOutputCommand;
import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.service.CsvFolderService;
import com.example.poc.service.CsvPaymentsFileService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

@SpringBootApplication
public class CsvPaymentsProofOfConceptApplication {
    private static ApplicationContext ctx;
    public static void main(String[] args) {
        ctx = SpringApplication.run(CsvPaymentsProofOfConceptApplication.class, args);
        ReadFolderCommand readFolderCommand = ctx.getBean(ReadFolderCommand.class);
        CsvFolderService csvFolderService = ctx.getBean(CsvFolderService.class);

        CsvFolder aNewFolder = new CsvFolder("/Users/mari/IdeaProjects/CSV Payments PoC/src/test/files");
        readFolderCommand.execute(aNewFolder);
        Optional<CsvFolder> aFolder = csvFolderService.findById(aNewFolder.getId());
        aFolder.ifPresent(CsvPaymentsProofOfConceptApplication::printRecords);
    }

    private static void printRecords(CsvFolder csvFolder) {
        CsvInputCommand csvInputCommand = ctx.getBean(CsvInputCommand.class);
        CsvPaymentsFileService csvPaymentsFileService = ctx.getBean(CsvPaymentsFileService.class);

        for (CsvPaymentsFile file: csvFolder.getFiles()) {
            System.out.println("Writing payment records to System.out...");
            csvInputCommand.execute(file).forEach(System.out::println);
            Optional<CsvPaymentsFile> aFile = csvPaymentsFileService.findById(file.getId());
            aFile.ifPresent(CsvPaymentsProofOfConceptApplication::writeCsvFiles);
        }
    }

    private static void writeCsvFiles(CsvPaymentsFile csvPaymentsFile) {
        CsvOutputCommand csvOutputCommand = ctx.getBean(CsvOutputCommand.class);

        System.out.println("Writing output CSV file...");
        csvOutputCommand.execute(csvPaymentsFile);
    }
}
