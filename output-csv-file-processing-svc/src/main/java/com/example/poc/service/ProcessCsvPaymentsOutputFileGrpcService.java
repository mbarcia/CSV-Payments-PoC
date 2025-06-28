package com.example.poc.service;

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.service.GrpcServiceClientStreamingAdapter;
import com.example.poc.common.service.Service;
import com.example.poc.grpc.MutinyProcessCsvPaymentsOutputFileServiceGrpc;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.common.mapper.PaymentOutputMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

import java.util.List;

@GrpcService
public class ProcessCsvPaymentsOutputFileGrpcService extends MutinyProcessCsvPaymentsOutputFileServiceGrpc.ProcessCsvPaymentsOutputFileServiceImplBase {

    @Inject
    ProcessCsvPaymentsOutputFileService domainService;

    @Inject
    CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

    @Inject
    PaymentOutputMapper paymentOutputMapper;

    @Override
    @Blocking
    public Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> remoteProcess(Multi<PaymentStatusSvc.PaymentOutput> grpcStream) {
        return new GrpcServiceClientStreamingAdapter<
                        PaymentStatusSvc.PaymentOutput,
                        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile,
                        PaymentOutput,
                        CsvPaymentsOutputFile>() {

            @Override
            protected Service<List<PaymentOutput>, CsvPaymentsOutputFile> getService() {
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
