package com.example.poc;

import com.example.poc.command.*;
import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentRecordOutputBean;
import com.example.poc.service.CsvPaymentsService;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
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
import java.util.stream.Stream;

@SpringBootApplication
public class CsvPaymentsProofOfConceptApplication implements CommandLineRunner {
    public static final String CSV_FOLDER = "csv/";
    @Autowired
    private CsvPaymentsService csvPaymentsService;
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

    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsProofOfConceptApplication.class);

    /**
     * @param args Folder path
     */
    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");
        SpringApplication.run(CsvPaymentsProofOfConceptApplication.class, args);
        LOG.info("APPLICATION FINISHED");
    }

    @Override
    public void run(String... args) throws Exception {
        // Create a CSV Folder object
        CsvFolder csvFolder = getCsvFolder(args);

        // Command the CSV folder object to be read and persisted in the DB
        readFolderCommand.execute(csvFolder);

        // Print CSV lines
        for (CsvPaymentsFile file : csvPaymentsService.findFilesByFolder(csvFolder)) {
            // Read the file, get a stream of records, and
            readFileCommand.execute(file).
                            // persist record to the DB
                            map(csvPaymentsService::persistRecord).
                            // post the payment record to the API service
                            map(sendPaymentCommand::execute).
                            // poll the API service for payment confirmation
                            map(pollPaymentStatusCommand::execute)
                            // and terminate the stream printing the record
                            .forEach(System.out::println);

            System.out.println("Writing output CSV file...");
            // Command the CSV file to be exported (from the DB)
            try (Writer writer = new FileWriter(file.getFilepath() + ".out")) {
                StatefulBeanToCsv<PaymentRecordOutputBean> sbc = new StatefulBeanToCsvBuilder<PaymentRecordOutputBean>(writer)
                        .withQuotechar('\'')
                        .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                        .build();
                // Get a stream of records which have been hydrated with the meta-data thanks to a query
                Stream<PaymentRecord> paymentRecordStream = csvPaymentsService.findRecordsByFile(file).stream();
                // Unparse each record to the bean structure and write it in order to get a CSV line
                sbc.write(paymentRecordStream.map(unParseRecordCommand::execute));
            } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
                throw new Exception(e);
            }
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
