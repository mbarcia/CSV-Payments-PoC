package com.example.poc.service;

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.common.mapper.PaymentOutputMapper;
import com.example.poc.common.service.GrpcServiceClientStreamingAdapter;
import com.example.poc.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import com.example.poc.grpc.PaymentStatusSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class ProcessCsvPaymentsOutputFileGrpcService extends MutinyProcessCsvPaymentsOutputFileServiceGrpc.ProcessCsvPaymentsOutputFileServiceImplBase {

    @Inject
    ProcessCsvPaymentsOutputFileReactiveService domainService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    PaymentOutputMapper paymentOutputMapper;

    @Override
    public Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> remoteProcess(Multi<PaymentStatusSvc.PaymentOutput> grpcStream) {
        return new GrpcServiceClientStreamingAdapter<
                        PaymentStatusSvc.PaymentOutput,
                        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile,
                        PaymentOutput,
                        CsvPaymentsOutputFile>() {

            @Override
            protected ProcessCsvPaymentsOutputFileReactiveService getService() {
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
        }.remoteProcess(grpcStream); // <-- send Multi<> input instead
    }
}
