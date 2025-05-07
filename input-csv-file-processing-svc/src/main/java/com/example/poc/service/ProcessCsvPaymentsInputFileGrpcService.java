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

import java.util.List;
import java.util.stream.Collectors;
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
            InputCsvFileProcessingSvc.PaymentRecordList ,
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
                protected InputCsvFileProcessingSvc.PaymentRecordList toGrpc(Stream<PaymentRecord> stream) {
                    List<InputCsvFileProcessingSvc.PaymentRecord> grpcList =
                            stream.map(paymentRecordMapper::toGrpc).collect(Collectors.toList());

                    return com.example.poc.grpc.InputCsvFileProcessingSvc.PaymentRecordList.newBuilder()
                            .addAllRecords(grpcList)
                            .build();
                }
            };

    public void remoteProcess(InputCsvFileProcessingSvc.CsvPaymentsInputFile request,
                       StreamObserver<InputCsvFileProcessingSvc.PaymentRecordList> responseObserver) {
        adapter.remoteProcess(request, responseObserver);
    }
}
