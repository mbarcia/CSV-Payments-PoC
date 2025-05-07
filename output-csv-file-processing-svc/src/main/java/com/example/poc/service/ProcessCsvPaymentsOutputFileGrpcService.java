package com.example.poc.service;

import com.example.poc.domain.CsvPaymentsInputFile;
import com.example.poc.domain.CsvPaymentsOutputFile;
import com.example.poc.domain.PaymentOutput;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.ProcessCsvPaymentsOutputFileServiceGrpc;
import com.example.poc.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.mapper.FilePairMapper;
import com.example.poc.mapper.PaymentOutputMapper;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

import java.util.Map;

@GrpcService
public class ProcessCsvPaymentsOutputFileGrpcService extends ProcessCsvPaymentsOutputFileServiceGrpc.ProcessCsvPaymentsOutputFileServiceImplBase {

    @Inject
    ProcessCsvPaymentsOutputFileService domainService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    PaymentOutputMapper paymentOutputMapper;

    @Inject
    FilePairMapper filePairMapper;

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

    public void remoteProcess(PaymentStatusSvc.PaymentOutput request,
                       StreamObserver<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> responseObserver) {
        adapter.remoteProcess(request, responseObserver);
    }

    public void initialiseFiles(OutputCsvFileProcessingSvc.InitialiseFilesRequest grpcRequest, StreamObserver<Empty> responseObserver) {
        Map<CsvPaymentsInputFile, CsvPaymentsOutputFile> map = filePairMapper.fromProtoList(grpcRequest);
        domainService.initialiseFiles(map);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    public void closeFiles(Empty request, StreamObserver<Empty> responseObserver) {
        domainService.closeFiles();

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
