package com.example.poc;

import com.example.poc.command.*;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecordOutputBean;
import com.example.poc.service.CsvPaymentsService;
import com.example.poc.service.CsvPaymentsServiceImpl;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.transaction.Transactional;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

@SpringBootApplication
public class CsvPaymentsProofOfConceptApplication {
    public static final String CSV_FOLDER = "csv/";
    private static CsvPaymentsService csvPaymentsService;
    private static ReadFolderCommand readFolderCommand;
    private static ReadFileCommand readFileCommand;
    private static SendPaymentCommand sendPaymentCommand;
    private static PollPaymentStatusCommand pollPaymentStatusCommand;
    private static UnparseRecordCommand unParseRecordCommand;

    /**
     * @param args Folder path
     */
    @Transactional
    public static void main(String[] args) {
        // Initialise components and services
        initialiseComponents(args);

        // Create a CSV Folder object
        CsvFolder detachedFolder = getCsvFolder(args);

        // Command the CSV folder object to be read and persisted in the DB
        readFolderCommand.execute(detachedFolder);
        csvPaymentsService.persistFolder(detachedFolder);
        CsvFolder csvFolder = getCsvFolder(detachedFolder);

        // Print CSV lines
        for (CsvPaymentsFile file : csvFolder.getFiles()) {
            // Actually process the file and print a result it to System.out
            // First step is read the file
            readFileCommand.execute(file).
                    // Save each record to the DB
                    map(csvPaymentsService::persistRecord).
                    // Post the payment record to the API service
                    map(sendPaymentCommand::execute).
                    // Poll the API service for payment confirmation
                    map(pollPaymentStatusCommand::execute)
                    // and terminate the stream printing each record
                    .forEach(System.out::println);

            System.out.println("Writing output CSV file...");
            // Command the CSV file to be exported (from the DB)
            CsvPaymentsFile paymentsFile = getCsvPaymentsFile(file);

            try (Writer writer = new FileWriter(paymentsFile.getFilepath() + ".out")) {
                StatefulBeanToCsv<PaymentRecordOutputBean> sbc = new StatefulBeanToCsvBuilder<PaymentRecordOutputBean>(writer)
                        .withQuotechar('\'')
                        .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                        .build();
                sbc.write(paymentsFile.getRecords().stream().map(unParseRecordCommand::execute));
            } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Done.");
        }
    }

    private static CsvFolder getCsvFolder(String[] args) {
        String folder = args.length == 0 ? CSV_FOLDER : args[0];
        URL resource = CsvPaymentsProofOfConceptApplication.class.getClassLoader().getResource(folder);
        assert resource != null;
        CsvFolder detachedFolder;
        try {
            detachedFolder = new CsvFolder(resource.toURI().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return detachedFolder;
    }

    private static CsvPaymentsFile getCsvPaymentsFile(CsvPaymentsFile file) {
        // Populate the files
        Optional<CsvPaymentsFile> fileById = csvPaymentsService.findFileById(file.getId());
        if (fileById.isEmpty()) {
            throw new RuntimeException("File not found in the DB");
        }
        return fileById.get();
    }

    private static CsvFolder getCsvFolder(CsvFolder detachedFolder) {
        // Populate the files
        Optional<CsvFolder> aNewFolder = csvPaymentsService.findFolderById(detachedFolder.getId());
        if (aNewFolder.isEmpty()) {
            throw new RuntimeException("Folder not found in the DB");
        }
        return aNewFolder.get();
    }

    private static void initialiseComponents(String[] args) {
        ApplicationContext ctx = SpringApplication.run(CsvPaymentsProofOfConceptApplication.class, args);
        csvPaymentsService = ctx.getBean(CsvPaymentsServiceImpl.class);
        readFolderCommand = ctx.getBean(ReadFolderCommand.class);
        readFileCommand = ctx.getBean(ReadFileCommand.class);
        sendPaymentCommand = ctx.getBean(SendPaymentCommand.class);
        pollPaymentStatusCommand = ctx.getBean(PollPaymentStatusCommand.class);
        unParseRecordCommand = ctx.getBean(UnparseRecordCommand.class);
    }
}
