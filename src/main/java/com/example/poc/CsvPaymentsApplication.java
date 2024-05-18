package com.example.poc;

import com.example.poc.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StopWatch;

import java.net.URISyntaxException;

@SpringBootApplication
public class CsvPaymentsApplication implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);
    private final ReadFolderService readFolderService;
    private final ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService;
    private final ProcessAckPaymentSentService processAckPaymentSentService;
    private final SendPaymentRecordService sendPaymentRecordService;
    private final ProcessPaymentOutputStreamService processPaymentOutputStreamService;
    private final ProcessPaymentStatusService processPaymentStatusService;

    public CsvPaymentsApplication(ReadFolderService readFolderService, ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService, ProcessAckPaymentSentService processAckPaymentSentService, SendPaymentRecordService sendPaymentRecordService, ProcessPaymentOutputStreamService processPaymentOutputStreamService, ProcessPaymentStatusService processPaymentStatusService) {
        this.readFolderService = readFolderService;
        this.processCsvPaymentsInputFileService = processCsvPaymentsInputFileService;
        this.processAckPaymentSentService = processAckPaymentSentService;
        this.sendPaymentRecordService = sendPaymentRecordService;
        this.processPaymentOutputStreamService = processPaymentOutputStreamService;
        this.processPaymentStatusService = processPaymentStatusService;
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
            // Return all files in the folder
            readFolderService.process(args)
                .parallel()
                // Process records of all files, one by one
                .map(processCsvPaymentsInputFileService::process)
                .map(processPaymentOutputStreamService::process)
                .toList();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        System.out.println("And these are the contents in the database:");

        readFolderService.print();
        processCsvPaymentsInputFileService.print();
        sendPaymentRecordService.print();
        processAckPaymentSentService.print();
        processPaymentStatusService.print();
        processPaymentOutputStreamService.print();
    }
}
