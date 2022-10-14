package com.example.poc;

import com.example.poc.command.*;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecordOutputBean;
import com.example.poc.service.CsvPaymentsService;
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

    /**
     * @param args Folder path
     */
    @Transactional
    public static void main(String[] args) {
        // Initialise components and services
        ApplicationContext ctx = SpringApplication.run(CsvPaymentsProofOfConceptApplication.class, args);
        CsvPaymentsService csvPaymentsService = ctx.getBean(CsvPaymentsService.class);
        ReadFolderCommand readFolderCommand = ctx.getBean(ReadFolderCommand.class);

        // Create a CSV Folder object
        String folder = args.length == 0 ? CSV_FOLDER : args[0];
        URL resource = CsvPaymentsProofOfConceptApplication.class.getClassLoader().getResource(folder);
        assert resource != null;
        CsvFolder detachedFolder;
        try {
            detachedFolder = new CsvFolder(resource.toURI().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Command the CSV folder object to be read and persisted in the DB
        readFolderCommand.execute(detachedFolder);

        // Populate the files
        Optional<CsvFolder> aNewFolder = csvPaymentsService.findFolderById(detachedFolder.getId());
        if (aNewFolder.isEmpty()) {
            throw new RuntimeException("Folder not found in the DB");
        }

        // Print CSV lines

        // Initialise components and services
        ReadFileCommand readFileCommand = ctx.getBean(ReadFileCommand.class);
        PersistRecordCommand persistRecordCommand = ctx.getBean(PersistRecordCommand.class);
        SendPaymentCommand sendPaymentCommand = ctx.getBean(SendPaymentCommand.class);
        PollPaymentStatusCommand pollPaymentStatusCommand = ctx.getBean(PollPaymentStatusCommand.class);

        // Do this for all the CSV files in the folder

        for (CsvPaymentsFile file : aNewFolder.get().getFiles()) {
            // Actually process the file and print a result it to System.out
            readFileCommand.execute(file).
                    map(persistRecordCommand::execute).
                    map(sendPaymentCommand::execute).
                    map(pollPaymentStatusCommand::execute).forEach(System.out::println);
            // Continue to poll the API
            // Initialise components and services
            UnparseRecordCommand unParseRecordCommand = ctx.getBean(UnparseRecordCommand.class);

            System.out.println("Writing output CSV file...");
            // Command the CSV file to be exported (from the DB)
            // Populate the files
            Optional<CsvPaymentsFile> fileById = csvPaymentsService.findFileById(file.getId());
            if (fileById.isEmpty()) {
                throw new RuntimeException("File not found in the DB");
            }

            try (Writer writer = new FileWriter(fileById.get().getFilepath() + ".out")) {
                StatefulBeanToCsv<PaymentRecordOutputBean> sbc = new StatefulBeanToCsvBuilder<PaymentRecordOutputBean>(writer)
                        .withQuotechar('\'')
                        .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                        .build();
                sbc.write(fileById.get().getRecords().stream().map(unParseRecordCommand::execute));
            } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Done.");
        }
    }
}
