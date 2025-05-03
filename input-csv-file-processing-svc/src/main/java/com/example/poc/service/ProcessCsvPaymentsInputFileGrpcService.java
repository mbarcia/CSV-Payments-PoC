package com.example.poc.service;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.ProcessCsvPaymentsInputFileServiceGrpc;
import com.example.poc.mappers.CsvPaymentsInputFileMapper;
import com.example.poc.mappers.PaymentRecordMapper;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

import java.util.stream.Stream;

@GrpcService
public class ProcessCsvPaymentsInputFileGrpcService extends ProcessCsvPaymentsInputFileServiceGrpc.ProcessCsvPaymentsInputFileServiceImplBase {

    @Inject
    ProcessCsvPaymentsInputFileService domainService;

    @Inject
    CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

    @Inject
    PaymentRecordMapper paymentRecordMapper;

    private final GrpcServiceAdapter<InputCsvFileProcessingSvc.CsvPaymentsInputFile,
            Stream<InputCsvFileProcessingSvc.PaymentRecord>,
            CsvPaymentsInputFile,
            Stream<PaymentRecord>> adapter =
            new GrpcServiceAdapter<>() {

                protected ProcessCsvPaymentsInputFileService getService() {
                    return domainService;
                }

                @Override
                protected CsvPaymentsInputFile fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputFile grpcIn) {
                    return csvPaymentsInputFileMapper.fromGrpc(grpcIn);
                }

                @Override
                protected Stream<InputCsvFileProcessingSvc.PaymentRecord> toGrpc(Stream<PaymentRecord> domainOut) {
                    return domainOut.map(paymentRecordMapper::toGrpc);
                }
            };

    public void invoke(InputCsvFileProcessingSvc.CsvPaymentsInputFile request,
                       StreamObserver<Stream<InputCsvFileProcessingSvc.PaymentRecord>> responseObserver) {
        adapter.invoke(request, responseObserver);
    }
}
