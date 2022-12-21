package com.example.poc;

import com.example.poc.command.*;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentOutput;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication
public class CsvPaymentsProofOfConceptApplication implements CommandLineRunner {
    public static final String CSV_FOLDER = "csv/";
    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsProofOfConceptApplication.class);
    @Autowired
    private ReadFolderCommand readFolderCommand;
    @Autowired
    private ReadFileCommand readFileCommand;
    @Autowired
    private SendPaymentCommand sendPaymentCommand;
    @Autowired
    private PollPaymentStatusCommand pollPaymentStatusCommand;
    @Autowired
    private UnparseRecordCommand unParseRecordCommand;

    /**
     * @param args Folder path
     */
    public static void main(String[] args) {
        LOG.info("APPLICATION BEGINS");
        SpringApplication.run(CsvPaymentsProofOfConceptApplication.class, args);
        LOG.info("APPLICATION FINISHED");
    }

    @Override
    public void run(String... args) {
        // Process all files in the folder
        try {
            processFiles(readFolderCommand.execute(getCsvFolder(args)).toList())
                    .forEach(System.out::println);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private List<CsvPaymentsFile> processFiles(List<CsvPaymentsFile> csvPaymentsFiles) {
        List<CsvPaymentsFile> result = new ArrayList<>();
        // Process each file in the folder
        for (CsvPaymentsFile file : csvPaymentsFiles) {
            try {
                result.add(processRecords(file));
            } catch (Throwable unchecked) {
                LOG.error(unchecked.getMessage());
            } // continue with the next file
        }

        return result;
    }

    @Transactional
    private CsvPaymentsFile processRecords(CsvPaymentsFile file) {
        try (Writer writer = new FileWriter(file.getFilepath() + ".out")) {
            // Create the CSV writer
            StatefulBeanToCsv<PaymentOutput> sbc = new StatefulBeanToCsvBuilder<PaymentOutput>(writer)
                    .withQuotechar('\'')
                    .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                    .build();
            // Read the file, get a stream of records, and
            Stream<PaymentOutput> paymentOutputStream = readFileCommand.execute(file)
                    // post the payment record to the API service
                    .map(sendPaymentCommand::execute)
                    // poll the API service for payment confirmation
                    .map(pollPaymentStatusCommand::execute)
                    // dump the transformed payment data into an output record
                    .map(unParseRecordCommand::execute);
            // stream output records to the new CSV file
            sbc.write(paymentOutputStream);

            // Note that if any runtime exception was thrown,
            // the stream processing stops and the output file is empty

            return file;
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CsvFolder getCsvFolder(String[] args) throws URISyntaxException {
        // Default to CSV_FOLDER if no param has been provided
        String folder = args.length == 0 ? CSV_FOLDER : args[0];
        // Get the file system resource
        URL resource = CsvPaymentsProofOfConceptApplication.class.getClassLoader().getResource(folder);
        assert resource != null;

        // Return the domain object
        return new CsvFolder(resource.toURI().getPath());
    }
}
