package com.example.poc.service;


import com.example.poc.client.CsvPaymentsApplication;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

@ApplicationScoped
public class OrchestratorService {

    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);

    String csvFolder;

    @Inject
    ReadFolderService readFolderService;

    @Inject
    ProcessCsvPaymentsInputFileService processCsvPaymentsInputFileService;

    @Inject
    ProcessAckPaymentSentService processAckPaymentSentService;

    @Inject
    SendPaymentRecordService sendPaymentRecordService;

    @Inject
    ProcessPaymentOutputService processPaymentOutputService;

    @Inject
    ProcessPaymentStatusService processPaymentStatusService;

    public void process(String csvFolder) {
        this.csvFolder = csvFolder;
        process();
    }

    public void process() {
        // Get a map of input/output files, obtained and created from the folder name
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> csvPaymentsOutputFileMap = getCsvPaymentsInputFiles(csvFolder);
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

    public List<CsvPaymentsOutputFile> getCsvPaymentsOutputFilesList(Stream<PaymentRecord> recordsStream) {
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

    private Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> getCsvPaymentsInputFiles(String csvFolder) {
        return readFolderService
                // Return all CSV files contained inside the folder
                .process(csvFolder);
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
