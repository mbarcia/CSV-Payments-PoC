package com.example.poc;

import com.example.poc.command.CsvInputCommand;
import com.example.poc.command.CsvOutputCommand;
import com.example.poc.command.PollPaymentCommand;
import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.service.CsvFolderService;
import com.example.poc.service.CsvPaymentsFileService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

@SpringBootApplication
public class CsvPaymentsProofOfConceptApplication {
    public static final String CSV_FOLDER = "csv/";
    private static ApplicationContext ctx;

    /**
     * @param args Folder path
     * @throws URISyntaxException Malformed folder path
     */
    public static void main(String[] args) throws URISyntaxException {
        // Initialise components and services
        ctx = SpringApplication.run(CsvPaymentsProofOfConceptApplication.class, args);
        ReadFolderCommand readFolderCommand = ctx.getBean(ReadFolderCommand.class);
        CsvFolderService csvFolderService = ctx.getBean(CsvFolderService.class);

        // Create a CSV Folder object
        String folder = args.length == 0 ? CSV_FOLDER : args[0];
        URL resource = CsvPaymentsProofOfConceptApplication.class.getClassLoader().getResource(folder);
        assert resource != null;
        CsvFolder aNewFolder = new CsvFolder(resource.toURI().getPath());

        // Command the CSV folder object to be read
        readFolderCommand.execute(aNewFolder);

        // Refresh the data from the DB
        Optional<CsvFolder> aFolder = csvFolderService.findById(aNewFolder.getId());

        // Print CSV lines
        aFolder.ifPresent(CsvPaymentsProofOfConceptApplication::printRecords);
    }

    private static void printRecords(CsvFolder csvFolder) {
        // Initialise components and services
        CsvInputCommand csvInputCommand = ctx.getBean(CsvInputCommand.class);
        CsvPaymentsFileService csvPaymentsFileService = ctx.getBean(CsvPaymentsFileService.class);

        // Do this for all the CSV files in the folder
        for (CsvPaymentsFile file: csvFolder.getFiles()) {
            // Actually process the file and print a result it to System.out
            csvInputCommand.execute(file).forEach(System.out::println);
            // Refresh the data from the DB
            Optional<CsvPaymentsFile> aFile = csvPaymentsFileService.findById(file.getId());
            // Continue to poll the API
            aFile.ifPresent(CsvPaymentsProofOfConceptApplication::asyncPollPaymentStatus);
        }
    }

    private static void asyncPollPaymentStatus(CsvPaymentsFile csvPaymentsFile) {
        // Initialise components and services
        PollPaymentCommand pollPaymentCommand = ctx.getBean(PollPaymentCommand.class);

        // Command the CSV file to poll the API for all its payment records
        System.out.println("Polling payments statuses...");
        pollPaymentCommand.execute(csvPaymentsFile);

        // Continue to export the CSV file
        writeCsvFiles(csvPaymentsFile);
    }

    private static void writeCsvFiles(CsvPaymentsFile csvPaymentsFile) {
        // Initialise components and services
        CsvOutputCommand csvOutputCommand = ctx.getBean(CsvOutputCommand.class);

        System.out.println("Writing output CSV file...");
        // Command the CSV file to be exported (from the DB)
        csvOutputCommand.execute(csvPaymentsFile);
        System.out.println("Done.");
    }
}
