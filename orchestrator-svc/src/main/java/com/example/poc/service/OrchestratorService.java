package com.example.poc.service;


import com.example.poc.common.domain.CsvFolder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        CsvFolder csvFolder = setupCsvFolder(csvFolderPath);

        List<Uni<CsvPaymentsOutputFile>> processingUnis = readFolder(csvFolder).keySet().stream()
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

    /*
     * Helper I/O methods
     */

    public File[] listCsvFiles(String directoryPath) {
        if (Objects.nonNull(directoryPath)) {
            File directory = new File(directoryPath);

            return directory.listFiles((_, name) -> name.toLowerCase().endsWith(".csv"));
        } else {
            return new File[0];
        }
    }

    private CsvFolder setupCsvFolder(String csvFolderPath) throws URISyntaxException, IllegalArgumentException {
        LOG.info("Setting up CSV folder path: {}", csvFolderPath);

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
            throw new IllegalArgumentException("Folder not found: " + csvFolderPath);
        }

        return new CsvFolder(resource.toURI().getPath());
    }

    public CsvPaymentsInputFile setupInputCsvFile(File file) {
        return new CsvPaymentsInputFile(file);
    }

    public CsvPaymentsOutputFile setupOutputCsvFile(CsvPaymentsInputFile inputFile) throws IOException {
        return new CsvPaymentsOutputFile(inputFile.getFilepath());
    }

    public Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> readFolder(CsvFolder csvFolder) {
        File[] files = listCsvFiles(csvFolder.getFolderPath());
        HashMap<CsvPaymentsInputFile, CsvPaymentsOutputFile> result = new HashMap<>();

        for (File file : files) {
            CsvPaymentsInputFile inputFile = setupInputCsvFile(file);
            try {
                CsvPaymentsOutputFile outputFile = setupOutputCsvFile(inputFile);
                result.put(inputFile, outputFile);
            } catch (IOException ignored) {
                // TODO ignoring this is probably not a good thing
            }
        }

        return result;
    }
}
