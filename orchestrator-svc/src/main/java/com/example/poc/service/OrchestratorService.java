package com.example.poc.service;


import com.example.poc.domain.CsvFolder;
import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.grpc.*;
import com.example.poc.mapper.*;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import io.grpc.Channel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.protobuf.Empty;

@ApplicationScoped
public class OrchestratorService {

    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);

    @Inject
    HybridResourceLoader resourceLoader;

    @Inject
    @GrpcClient("process-csv-payments-input-file")
    Channel channel;

    private ProcessCsvPaymentsInputFileServiceGrpc.ProcessCsvPaymentsInputFileServiceStub processCsvPaymentsInputFileServiceStub;

    @PostConstruct
    void init() {
        processCsvPaymentsInputFileServiceStub = ProcessCsvPaymentsInputFileServiceGrpc.newStub(channel);
    }

    @Inject
    @GrpcClient("process-ack-payment-sent")
    ProcessAckPaymentSentServiceGrpc.ProcessAckPaymentSentServiceBlockingStub processAckPaymentSentService;

    @Inject
    @GrpcClient("send-payment-record")
    SendPaymentRecordServiceGrpc.SendPaymentRecordServiceBlockingStub sendPaymentRecordService;

    @Inject
    @GrpcClient("process-csv-payments-output-file")
    ProcessCsvPaymentsOutputFileServiceGrpc.ProcessCsvPaymentsOutputFileServiceBlockingStub processCsvPaymentsOutputFileService;

    @Inject
    @GrpcClient("process-payment-status")
    ProcessPaymentStatusServiceGrpc.ProcessPaymentStatusServiceBlockingStub processPaymentStatusService;

    @Inject
    FilePairMapper filePairMapper;

    @Inject
    PaymentRecordMapper paymentRecordMapper;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

    public void process(String csvFolderPath) throws URISyntaxException {
        CsvFolder csvFolder = getCsvFolder(csvFolderPath);
        // Get a map of input/output files, obtained and created from the folder name
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> csvPaymentsOutputFileMap = readFolder(csvFolder);
        // Initialise the service with this map
        OutputCsvFileProcessingSvc.InitialiseFilesRequest request = filePairMapper.toProtoList(csvPaymentsOutputFileMap);
        //noinspection ResultOfMethodCallIgnored
        processCsvPaymentsOutputFileService.initialiseFiles(request);
        // Get a stream of CSV record objects coming from all the files in the folder
        List<CompletableFuture<List<PaymentRecord>>> futureList = getSingleRecordStream(csvPaymentsOutputFileMap.keySet());

        // Wait for all futures to complete
        CompletableFuture<Void> allDoneFuture =
                CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));

        // Block until all are done (you could make this async too if needed)
        allDoneFuture.join();

        // Gather all results (now safe because we waited)
        Stream<List<PaymentRecord>> allRecordsStreams = futureList.stream()
                .map(CompletableFuture::join);          // each gives List<PaymentRecord>

        // Process the stream of CSV record objects
        for (List<PaymentRecord> recordStream : allRecordsStreams.toList()) {
            // This is where most part of the processing takes place (in parallel)
            List<CsvPaymentsOutputFile> outputFilesList = this.getCsvPaymentsOutputFilesList(recordStream);
            LOG.info("The resulting output files are: {}", Arrays.toString(outputFilesList.toArray()));
        }

        // Flush/close the output file buffers
        //noinspection ResultOfMethodCallIgnored
        processCsvPaymentsOutputFileService.closeFiles(Empty.getDefaultInstance());
    }

    private CsvFolder getCsvFolder(String csvFolderPath) throws URISyntaxException, IllegalArgumentException {
        LOG.info("Processing CSV folder path: {}", csvFolderPath);

        // In development, this might list example files from the JAR
        // In production, it will list real files from the external directory
        List<URL> csvFiles = resourceLoader.listResources(csvFolderPath);

        if (csvFiles.isEmpty()) {
            LOG.warn("No CSV files found in {}", csvFolderPath);
            // Add diagnostic info if needed
            resourceLoader.diagnoseResourceAccess(csvFolderPath);
        }

        URL resource = resourceLoader.getResource(csvFolderPath);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + csvFolderPath);
        }

        return new CsvFolder(resource.toURI().getPath());
    }

    public List<CsvPaymentsOutputFile> getCsvPaymentsOutputFilesList(List<PaymentRecord> records) {
        List<CsvPaymentsOutputFile> results = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<CsvPaymentsOutputFile>> futures = records.stream()
                    .map(record -> executor.submit(() -> {
                        // The processing pipeline for each record
                        var sent = sendPaymentRecordService.remoteProcess(paymentRecordMapper.toGrpc(record));
                        var acked = processAckPaymentSentService.remoteProcess(sent);
                        var statusProcessed = processPaymentStatusService.remoteProcess(acked);
                        return csvPaymentsOutputFileMapper.fromGrpc(processCsvPaymentsOutputFileService.remoteProcess(statusProcessed));
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

    List<CompletableFuture<List<PaymentRecord>>> getSingleRecordStream(Set<CsvPaymentsInputFile> inputFiles) {
        return inputFiles.stream()
                .map(this::callRemoteProcess)
                .collect(Collectors.toList());
    }

    private CompletableFuture<List<PaymentRecord>> callRemoteProcess(CsvPaymentsInputFile inputFile) {
        CompletableFuture<List<PaymentRecord>> future = new CompletableFuture<>();
        List<PaymentRecord> records = new ArrayList<>();
        InputCsvFileProcessingSvc.CsvPaymentsInputFile protoInputFile = csvPaymentsInputFileMapper.toGrpc(inputFile);

        processCsvPaymentsInputFileServiceStub.remoteProcess(protoInputFile, new StreamObserver<>() {
            @Override
            public void onNext(InputCsvFileProcessingSvc.PaymentRecord paymentRecord) {
                records.add(paymentRecordMapper.fromGrpc(paymentRecord));
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                future.complete(records);
            }
        });

        return future;
    }

    public File[] listCsvFiles(String directoryPath) {
        if (Objects.nonNull(directoryPath)) {
            File directory = new File(directoryPath);

            return directory.listFiles((file, name) -> name.toLowerCase().endsWith(".csv"));
        } else {
            return new File[0];
        }
    }

    public CsvPaymentsInputFile createInputCsvFile(File file) {
        return new CsvPaymentsInputFile(file);
    }

    public CsvPaymentsOutputFile createOutputCsvFile(CsvPaymentsInputFile inputFile) throws IOException {
        return new CsvPaymentsOutputFile(inputFile);
    }

    public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> readFolder(CsvFolder csvFolder) {
        File[] files = listCsvFiles(csvFolder.getFolderPath());
        HashMap<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = new HashMap<>();

        for (File file : files) {
            CsvPaymentsInputFile inputFile = createInputCsvFile(file);
            inputFile.setCsvFolder(csvFolder);
            try {
                CsvPaymentsOutputFile outputFile = createOutputCsvFile(inputFile);
                result.put(inputFile, outputFile);
            } catch (IOException ignored) {
                // TODO ignoring this is probably not a good thing
            }
        }

        return result;
    }
}
