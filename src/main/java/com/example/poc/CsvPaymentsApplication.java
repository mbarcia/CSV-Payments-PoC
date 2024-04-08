package com.example.poc;

import com.example.poc.command.ProcessFileCommand;
import com.example.poc.command.ReadFolderCommand;
import com.example.poc.domain.CsvFolder;
import com.example.poc.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StopWatch;

import java.net.URISyntaxException;
import java.net.URL;

@SpringBootApplication
public class CsvPaymentsApplication implements CommandLineRunner {
    public static final String CSV_FOLDER = "csv/";
    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);
    private final ReadFolderCommand readFolderCommand;
    private final ProcessFileCommand processFileCommand;
    private final AckPaymentSentRepository ackPaymentSentRepository;
    private final CsvFolderRepository csvFolderRepository;
    private final CsvPaymentsFileRepository csvPaymentsFileRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentStatusRepository paymentStatusRepository;

    @Autowired
    public CsvPaymentsApplication(ReadFolderCommand readFolderCommand, ProcessFileCommand processFileCommand, AckPaymentSentRepository ackPaymentSentRepository, CsvFolderRepository csvFolderRepository, CsvPaymentsFileRepository csvPaymentsFileRepository, PaymentRecordRepository paymentRecordRepository, PaymentStatusRepository paymentStatusRepository) {
        this.readFolderCommand = readFolderCommand;
        this.processFileCommand = processFileCommand;
        this.ackPaymentSentRepository = ackPaymentSentRepository;
        this.csvFolderRepository = csvFolderRepository;
        this.csvPaymentsFileRepository = csvPaymentsFileRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentStatusRepository = paymentStatusRepository;
    }

    /**
     * @param args Folder path
     */
    public static void main(String[] args) {
        LOG.info("APPLICATION BEGINS");
        StopWatch watch = new StopWatch();
        watch.start();
        SpringApplication.run(CsvPaymentsApplication.class, args);
        watch.stop();
        LOG.info("APPLICATION FINISHED in {} seconds", watch.getTotalTimeSeconds());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void run(String... args) {
        try {
            CsvFolder csvFolder = getCsvFolder(args);
            // Return all files in the folder
            readFolderCommand.execute(csvFolder)
                    .parallel()
                    // Process records of all files, one by one
                    .map(processFileCommand::execute).toList();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        System.out.println("And these are the contents in the database:");
        csvFolderRepository.findAll().forEach(System.out::println);
        csvPaymentsFileRepository.findAll().forEach(System.out::println);
        paymentRecordRepository.findAll().forEach(System.out::println);
        ackPaymentSentRepository.findAll().forEach(System.out::println);
        paymentStatusRepository.findAll().forEach(System.out::println);
    }

    private CsvFolder getCsvFolder(String[] args) throws URISyntaxException {
        // Default to CSV_FOLDER if no param has been provided
        String folder = args.length == 0 ? CSV_FOLDER : args[0];
        // Get the file system resource
        URL resource = CsvPaymentsApplication.class.getClassLoader().getResource(folder);
        assert resource != null;

        // Return the domain object
        return new CsvFolder(resource.toURI().getPath());
    }
}
