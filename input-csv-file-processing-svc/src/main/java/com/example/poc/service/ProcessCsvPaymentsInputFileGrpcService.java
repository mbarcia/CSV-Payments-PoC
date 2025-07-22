package com.example.poc.service;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import com.example.poc.common.service.GrpcServiceStreamingAdapter;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.MutinyProcessCsvPaymentsInputFileServiceGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;

@GrpcService
public class ProcessCsvPaymentsInputFileGrpcService
        extends MutinyProcessCsvPaymentsInputFileServiceGrpc.ProcessCsvPaymentsInputFileServiceImplBase {

    @Inject
    ProcessCsvPaymentsInputFileReactiveService domainService;

    @Inject
    CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

    @Inject
    PaymentRecordMapper paymentRecordMapper;

    @Override
    public Multi<InputCsvFileProcessingSvc.PaymentRecord> remoteProcess(
            InputCsvFileProcessingSvc.CsvPaymentsInputFile request) {

        return new GrpcServiceStreamingAdapter<
                        InputCsvFileProcessingSvc.CsvPaymentsInputFile,                      // GrpcIn
                        InputCsvFileProcessingSvc.PaymentRecord,                            // GrpcOut
                        CsvPaymentsInputFile,                                               // DomainIn
                        PaymentRecord>()                                                    // DomainOut
        {
            @Override
            protected ProcessCsvPaymentsInputFileReactiveService getService() {
                return domainService;
            }

            @Override
            protected CsvPaymentsInputFile fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputFile grpcIn) {
                return csvPaymentsInputFileMapper.fromGrpc(grpcIn);
            }

            @Override
            protected InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecord domainOut) {
                return paymentRecordMapper.toGrpc(paymentRecordMapper.toDto(domainOut));
            }
        }.remoteProcess(request);
    }
}
