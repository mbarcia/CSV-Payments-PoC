package com.example.poc.service;


import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class OrchestratorService {

    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);

    public static final Executor VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Inject
    HybridResourceLoader resourceLoader;

    @Inject
    @GrpcClient("process-csv-payments-input-file")
    MutinyProcessCsvPaymentsInputFileServiceGrpc.MutinyProcessCsvPaymentsInputFileServiceStub processCsvPaymentsInputFileService;

    @Inject
    @GrpcClient("process-ack-payment-sent")
    MutinyProcessAckPaymentSentServiceGrpc.MutinyProcessAckPaymentSentServiceStub processAckPaymentSentService;

    @Inject
    @GrpcClient("send-payment-record")
    MutinySendPaymentRecordServiceGrpc.MutinySendPaymentRecordServiceStub sendPaymentRecordService;

    @Inject
    @GrpcClient("process-csv-payments-output-file")
    MutinyProcessCsvPaymentsOutputFileServiceGrpc.MutinyProcessCsvPaymentsOutputFileServiceStub processCsvPaymentsOutputFileService;

    @Inject
    @GrpcClient("process-payment-status")
    MutinyProcessPaymentStatusServiceGrpc.MutinyProcessPaymentStatusServiceStub processPaymentStatusService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

    // --- CONCURRENCY LIMITS ---
    // These directly apply to the merge operator.
    private static final int CONCURRENCY_LIMIT_RECORDS = 200;
    private static final int CONCURRENCY_LIMIT_ACKS = 200; // Note the current mock provider throttles at 100
    private static final int CONCURRENCY_LIMIT_STATUSES = 200; // Note the current mock provider throttles at 100

    // --- RETRY SETTINGS ---
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMillis(100);

    public Uni<Void> process(String csvFolderPath) throws URISyntaxException {
        List<Uni<CsvPaymentsOutputFile>> processingUnis = readCsvFolder(csvFolderPath).keySet().stream()
            .map(inputFile ->
                    Uni.createFrom().item(inputFile)
                        .runSubscriptionOn(VIRTUAL_EXECUTOR)  // Shift execution to virtual thread
                        .flatMap(this::processOneFile)
                )
            .toList();

        return Uni.combine().all().unis(processingUnis).discardItems();
    }

    private Uni<CsvPaymentsOutputFile> processOneFile(CsvPaymentsInputFile inputFile) {
        LOG.info("🧵 {} running on thread: {}", inputFile, Thread.currentThread());

        Multi<InputCsvFileProcessingSvc.PaymentRecord> inputRecords = processCsvPaymentsInputFileService
            .remoteProcess(csvPaymentsInputFileMapper.toGrpc(inputFile));

        // Stage 1: Process all records with sendPaymentRecordService in parallel, with concurrency limit
        Multi<PaymentsProcessingSvc.AckPaymentSent> ackPaymentSentMulti = inputRecords
                .onItem().transformToUni(record -> // Transform each record into a Uni for processing
                        Uni.createFrom().item(record)
                                .runSubscriptionOn(VIRTUAL_EXECUTOR) // Execute processing on a virtual thread
                                .flatMap(sendPaymentRecordService::remoteProcess) // Step 1
                                // Add retry with backoff for transient throttling errors
                                .onFailure(this::isThrottlingError)
                                .retry().withBackOff(INITIAL_RETRY_DELAY, INITIAL_RETRY_DELAY.multipliedBy(2)).atMost(MAX_RETRIES)
                )
                .merge(CONCURRENCY_LIMIT_RECORDS); // Ensures concurrency limit

        // Collect results of Stage 1 to ensure all are complete before Stage 2
        Multi<PaymentsProcessingSvc.AckPaymentSent> ackPaymentSentMultiIntermediate = ackPaymentSentMulti
                .collect().asList()
                .onItem().transformToMulti(Multi.createFrom()::iterable); // convert List to Multi

        // Stage 2: Process all results from Stage 1 with processAckPaymentSentService in parallel, with concurrency limit
        Multi<PaymentsProcessingSvc.PaymentStatus> paymentStatusMulti = ackPaymentSentMultiIntermediate
                .onItem().transformToUni(record -> // Transform each record into a Uni for processing
                        Uni.createFrom().item(record)
                                .runSubscriptionOn(VIRTUAL_EXECUTOR)
                                .flatMap(processAckPaymentSentService::remoteProcess) // Step 2
                                .onFailure(this::isThrottlingError)
                                .retry().withBackOff(INITIAL_RETRY_DELAY, INITIAL_RETRY_DELAY.multipliedBy(2)).atMost(MAX_RETRIES)
                )
                .merge(CONCURRENCY_LIMIT_ACKS); // Ensures concurrency limit

        // Collect results of Stage 2 to ensure all are complete before Stage 3
        Multi<PaymentsProcessingSvc.PaymentStatus> paymentStatusMultiIntermediate = paymentStatusMulti
                .collect().asList()
                .onItem().transformToMulti(Multi.createFrom()::iterable);

        // Stage 3: Process all results from Stage 2 with processPaymentStatusService in parallel, with concurrency limit
        Multi<PaymentStatusSvc.PaymentOutput> paymentOutputMulti = paymentStatusMultiIntermediate
                .onItem().transformToUni(record -> // Transform each record into a Uni for processing
                        Uni.createFrom().item(record)
                                .runSubscriptionOn(VIRTUAL_EXECUTOR)
                                .flatMap(processPaymentStatusService::remoteProcess) // Step 3
                )
                .merge(CONCURRENCY_LIMIT_STATUSES); // Ensures concurrency limit

        // Collect results of Stage 3 to ensure all are complete before sending to output file
        Multi<PaymentStatusSvc.PaymentOutput> paymentOutputMultiIntermediate = paymentOutputMulti
                .collect().asList()
                .onItem().transformToMulti(Multi.createFrom()::iterable);

        return processCsvPaymentsOutputFileService.remoteProcess(paymentOutputMultiIntermediate)
            .onItem().transform(csvPaymentsOutputFileMapper::fromGrpc)
            .onItem().invoke(result -> LOG.info("✅ Completed processing: {}", result))
            .onFailure().invoke(e -> LOG.error("❌ Processing failed for: {}", inputFile, e));
    }

    public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> readCsvFolder(String csvFolderPath) throws URISyntaxException {
        LOG.info("Reading CSV folder from path: {}", csvFolderPath);

        URL resource = resourceLoader.getResource(csvFolderPath);
        if (resource == null) {
            throw new IllegalArgumentException(MessageFormat.format("CSV folder not found: {0}", csvFolderPath));
        }

        File directory = new File(resource.toURI());
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException(MessageFormat.format("CSV path is not a valid directory: {0}", directory.getAbsolutePath()));
        }

        File[] csvFiles = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            LOG.warn("No CSV files found in {}", csvFolderPath);
            resourceLoader.diagnoseResourceAccess(csvFolderPath);
            return Collections.emptyMap();
        }

        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = new HashMap<>();
        for (File file : csvFiles) {
            CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(file);
            try {
                CsvPaymentsOutputFile outputFile = new CsvPaymentsOutputFile(inputFile.getFilepath());
                result.put(inputFile, outputFile);
            } catch (IOException e) {
                LOG.warn("Failed to setup output file for: {}", file.getAbsolutePath(), e);
            }
        }

        return result;
    }

    // Helper predicate for checking gRPC throttling errors
    private boolean isThrottlingError(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException grpcEx) {
            Status.Code code = grpcEx.getStatus().getCode();
            // Common gRPC status codes for throttling/resource exhaustion:
            // RESOURCE_EXHAUSTED: The system is out of resources, or the request is rejected by a rate limit.
            // UNAVAILABLE: The service is currently unavailable. This is most likely a transient condition.
            // ABORTED: The operation was aborted, typically due to a concurrency issue like a transaction abort.
            return code == Status.Code.RESOURCE_EXHAUSTED || code == Status.Code.UNAVAILABLE || code == Status.Code.ABORTED;
        }

        return false;
    }
}