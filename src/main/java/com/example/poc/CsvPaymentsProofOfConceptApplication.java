package com.example.poc;

import com.example.poc.command.CsvInputCommand;
import com.example.poc.command.CsvOutputCommand;
import com.example.poc.command.PollPaymentCommand;
import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.service.CsvPaymentsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.transaction.Transactional;
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

        // Create a CSV Folder object
        String folder = args.length == 0 ? CSV_FOLDER : args[0];
        URL resource = CsvPaymentsProofOfConceptApplication.class.getClassLoader().getResource(folder);
        assert resource != null;
        CsvFolder aNewFolder = new CsvFolder(resource.toURI().getPath());

        // Command the CSV folder object to be read and persisted in the DB
        readFolderCommand.execute(aNewFolder);

        // Print CSV lines
        printRecords(aNewFolder);
    }

    @Transactional
    private static void printRecords(CsvFolder detachedFolder) {
        // Initialise components and services
        CsvPaymentsService service = ctx.getBean(CsvPaymentsService.class);
        CsvInputCommand csvInputCommand = ctx.getBean(CsvInputCommand.class);

        // Refresh the folder data from the DB
        Optional<CsvFolder> aFolder = service.findFolderById(detachedFolder.getId());

        aFolder.ifPresent(folder -> {
            // Do this for all the CSV files in the folder
            for (CsvPaymentsFile file : folder.getFiles()) {
                // Actually process the file and print a result it to System.out
                csvInputCommand.execute(file).forEach(System.out::println);
                // Continue to poll the API
                asyncPollPaymentStatus(file);
            }
        });
    }

    @Transactional
    private static void asyncPollPaymentStatus(CsvPaymentsFile detachedFile) {
        // Initialise components and services
        CsvPaymentsService csvPaymentsService = ctx.getBean(CsvPaymentsService.class);
        PollPaymentCommand pollPaymentCommand = ctx.getBean(PollPaymentCommand.class);

        // Refresh the file data from the DB
        Optional<CsvPaymentsFile> aFile = csvPaymentsService.findFileById(detachedFile.getId());

        aFile.ifPresentOrElse(f -> {
            // Command the CSV file to poll the API for all its payment records
            System.out.println("Polling payments statuses...");
            pollPaymentCommand.execute(f);

            // Continue to export the CSV file
            writeCsvFiles(f);
        }, () -> System.out.println("File not found in the DB"));
    }

    @Transactional
    private static void writeCsvFiles(CsvPaymentsFile csvPaymentsFile) {
        // Initialise components and services
        CsvOutputCommand csvOutputCommand = ctx.getBean(CsvOutputCommand.class);

        System.out.println("Writing output CSV file...");
        // Command the CSV file to be exported (from the DB)
        csvOutputCommand.execute(csvPaymentsFile);
        System.out.println("Done.");
    }
}
