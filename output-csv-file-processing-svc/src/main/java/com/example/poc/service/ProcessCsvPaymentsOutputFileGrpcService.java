package com.example.poc.service;

import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.ProcessCsvPaymentsInputFileServiceGrpc;
import com.example.poc.mappers.CsvPaymentsOutputFileMapper;
import com.example.poc.mappers.PaymentOutputMapper;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

@GrpcService
public class ProcessCsvPaymentsOutputFileGrpcService extends ProcessCsvPaymentsInputFileServiceGrpc.ProcessCsvPaymentsInputFileServiceImplBase {

    @Inject
    ProcessCsvPaymentsOutputFileService domainService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    PaymentOutputMapper paymentOutputMapper;

    private final GrpcServiceAdapter<PaymentStatusSvc.PaymentOutput,
            OutputCsvFileProcessingSvc.CsvPaymentsOutputFile,
            PaymentOutput,
            CsvPaymentsOutputFile> adapter =
            new GrpcServiceAdapter<>() {

                protected ProcessCsvPaymentsOutputFileService getService() {
                    return domainService;
                }

                @Override
                protected PaymentOutput fromGrpc(PaymentStatusSvc.PaymentOutput grpcIn) {
                    return paymentOutputMapper.fromGrpc(grpcIn);
                }

                @Override
                protected OutputCsvFileProcessingSvc.CsvPaymentsOutputFile toGrpc(CsvPaymentsOutputFile domainOut) {
                    return csvPaymentsOutputFileMapper.toGrpc(domainOut);
                }
            };

    public void invoke(PaymentStatusSvc.PaymentOutput request,
                       StreamObserver<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> responseObserver) {
        adapter.invoke(request, responseObserver);
    }
}
