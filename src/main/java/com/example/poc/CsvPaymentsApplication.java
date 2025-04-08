package com.example.poc;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@SpringBootApplication
public class CsvPaymentsApplication implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);
    private final ReadFolderService readFolderService;
    private final ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService;
    private final ProcessAckPaymentSentService processAckPaymentSentService;
    private final SendPaymentRecordService sendPaymentRecordService;
    private final ProcessPaymentOutputService processPaymentOutputService;
    private final ProcessPaymentStatusService processPaymentStatusService;

    public CsvPaymentsApplication(ReadFolderService readFolderService, ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService, ProcessAckPaymentSentService processAckPaymentSentService, SendPaymentRecordService sendPaymentRecordService, ProcessPaymentOutputService processPaymentOutputService, ProcessPaymentStatusService processPaymentStatusService) {
        this.readFolderService = readFolderService;
        this.processCsvPaymentsInputFileService = processCsvPaymentsInputFileService;
        this.processAckPaymentSentService = processAckPaymentSentService;
        this.sendPaymentRecordService = sendPaymentRecordService;
        this.processPaymentOutputService = processPaymentOutputService;
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

    @Override
    public void run(String... args) {
        // Get a map of input/output files, obtained and created from the folder name
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> csvPaymentsOutputFileMap = getCsvPaymentsInputFiles(args);
        // Initialise the service with this map
        processPaymentOutputService.initialiseFiles(csvPaymentsOutputFileMap);
        // Get a stream of CSV record objects coming from all the files in the folder
        Stream<Stream<PaymentRecord>> csvFilesStream = getSingleRecordStream(csvPaymentsOutputFileMap.keySet());
        // Process the stream of CSV record objects
        for (Stream<PaymentRecord> recordStream : csvFilesStream.toList()) {
            // This is where most part of the processing takes place (in parallel)
            List<CsvPaymentsOutputFile> outputFilesList = this.getCsvPaymentsOutputFilesList(recordStream);
            LOG.info("The resulting output files are: {}", Arrays.toString(outputFilesList.toArray()));
        }

        // Flush/close the output file buffers
        processPaymentOutputService.closeFiles(csvPaymentsOutputFileMap.values());
        printOutputToConsole();
    }

    List<CsvPaymentsOutputFile> getCsvPaymentsOutputFilesList(Stream<PaymentRecord> recordsStream) {
        List<PaymentRecord> records = recordsStream.toList(); // Collect records to process
        List<CsvPaymentsOutputFile> results = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<CsvPaymentsOutputFile>> futures = records.stream()
                    .map(record -> executor.submit(() -> {
                        // The processing pipeline for each record
                        var sent = sendPaymentRecordService.process(record);
                        var acked = processAckPaymentSentService.process(sent);
                        var statusProcessed = processPaymentStatusService.process(acked);
                        return processPaymentOutputService.process(statusProcessed);
                    }))
                    .toList();

            // Collect results from all futures
            for (Future<CsvPaymentsOutputFile> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    // Handle exceptions appropriately
                    throw new RuntimeException("Error processing payment record", e);
                }
            }
        }

        return results;
    }

    private Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> getCsvPaymentsInputFiles(String... args) {
        return readFolderService
            // Return all CSV files contained inside the folder
            .process(args);
    }

    private Stream<Stream<PaymentRecord>> getSingleRecordStream(Set<CsvPaymentsInputFile> csvFilesStream) {
        return csvFilesStream.stream()
            // Return all payment records in all CSV input files
            .map(processCsvPaymentsInputFileService::process);
    }

    private void printOutputToConsole() {
        System.out.println("And these are the contents in the database:");
        readFolderService.print();
        processCsvPaymentsInputFileService.print();
        sendPaymentRecordService.print();
        processAckPaymentSentService.print();
        processPaymentStatusService.print();
        processPaymentOutputService.print();
    }
}
