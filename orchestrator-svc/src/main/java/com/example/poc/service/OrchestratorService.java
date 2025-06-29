package com.example.poc.service;


import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.grpc.*;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OrchestratorService {

    private static final Logger LOG = LoggerFactory
            .getLogger(CsvPaymentsApplication.class);

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

    public Uni<Void> process(String csvFolderPath) throws URISyntaxException {
        List<Uni<CsvPaymentsOutputFile>> processingUnis = readCsvFolder(csvFolderPath).keySet().stream()
                .map(this::processOneFile)
                .toList();

        return Uni.combine().all().unis(processingUnis).discardItems();
    }

    private Uni<CsvPaymentsOutputFile> processOneFile(CsvPaymentsInputFile inputFile) {
        Multi<InputCsvFileProcessingSvc.PaymentRecord> inputRecords = processCsvPaymentsInputFileService
                .remoteProcess(csvPaymentsInputFileMapper.toGrpc(inputFile));

        Multi<PaymentsProcessingSvc.AckPaymentSent> acks = inputRecords
                .onItem().transformToUniAndConcatenate(record -> sendPaymentRecordService.remoteProcess(record));

        Multi<PaymentsProcessingSvc.PaymentStatus> statuses = acks
                .onItem().transformToUniAndConcatenate(ack -> processAckPaymentSentService.remoteProcess(ack));

        Multi<PaymentStatusSvc.PaymentOutput> outputs = statuses
                .onItem().transformToUniAndConcatenate(status -> processPaymentStatusService.remoteProcess(status));

        return processCsvPaymentsOutputFileService.remoteProcess(outputs)
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
                // Consider logging the error
                LOG.warn("Failed to setup output file for: {}", file.getAbsolutePath(), e);
            }
        }

        return result;
    }}
