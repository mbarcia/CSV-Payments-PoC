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
    private final ProcessFileService processFileService;
    private final ProcessRecordService processRecordService;
    private final SendPaymentService sendPaymentService;
    private final PollPaymentStatusService pollPaymentStatusService;
    private final UnparseRecordService unparseRecordService;

    public CsvPaymentsApplication(ReadFolderService readFolderService, ProcessFileService processFileService, ProcessRecordService processRecordService, SendPaymentService sendPaymentService, PollPaymentStatusService pollPaymentStatusService, UnparseRecordService unparseRecordService) {
        this.readFolderService = readFolderService;
        this.processFileService = processFileService;
        this.processRecordService = processRecordService;
        this.sendPaymentService = sendPaymentService;
        this.pollPaymentStatusService = pollPaymentStatusService;
        this.unparseRecordService = unparseRecordService;
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
                    .map(processFileService::process).toList();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        System.out.println("And these are the contents in the database:");

        readFolderService.print();
        processFileService.print();
        processRecordService.print();
        sendPaymentService.print();
        pollPaymentStatusService.print();
        unparseRecordService.print();
    }
}
