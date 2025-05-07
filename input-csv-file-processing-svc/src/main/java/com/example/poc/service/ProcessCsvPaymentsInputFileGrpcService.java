package com.example.poc.service;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.ProcessCsvPaymentsInputFileServiceGrpc;
import com.example.poc.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.mapper.PaymentRecordMapper;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

@GrpcService
public class ProcessCsvPaymentsInputFileGrpcService extends ProcessCsvPaymentsInputFileServiceGrpc.ProcessCsvPaymentsInputFileServiceImplBase {

    @Inject
    ProcessCsvPaymentsInputFileService domainService;

    @Inject
    CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

    @Inject
    PaymentRecordMapper paymentRecordMapper;

    private final GrpcServiceStreamingAdapter<InputCsvFileProcessingSvc.CsvPaymentsInputFile,
            InputCsvFileProcessingSvc.PaymentRecord,
            CsvPaymentsInputFile,
            PaymentRecord> adapter =
            new GrpcServiceStreamingAdapter<>() {

                protected ProcessCsvPaymentsInputFileService getService() {
                    return domainService;
                }

                @Override
                protected CsvPaymentsInputFile fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputFile grpcIn) {
                    return csvPaymentsInputFileMapper.fromGrpc(grpcIn);
                }

                @Override
                protected InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecord grpcOut) {
                    return paymentRecordMapper.toGrpc(grpcOut);
                }
            };

    public void remoteProcess(InputCsvFileProcessingSvc.CsvPaymentsInputFile request,
                       StreamObserver<InputCsvFileProcessingSvc.PaymentRecord> responseObserver) {
        adapter.remoteProcess(request, responseObserver);
    }
}
