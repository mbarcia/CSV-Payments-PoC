package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import com.example.poc.grpc.SendPaymentRecordServiceGrpc;
import com.example.poc.mapper.AckPaymentSentMapper;
import com.example.poc.mapper.PaymentRecordMapper;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

@GrpcService
public class SendPaymentRecordGrpcService extends SendPaymentRecordServiceGrpc.SendPaymentRecordServiceImplBase {

    @Inject
    SendPaymentRecordService domainService;

    @Inject
    PaymentRecordMapper paymentRecordMapper;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    private final GrpcServiceAdapter<InputCsvFileProcessingSvc.PaymentRecord,
            PaymentsProcessingSvc.AckPaymentSent,
            PaymentRecord,
            AckPaymentSent
            > adapter =
            new GrpcServiceAdapter<>() {

                protected SendPaymentRecordService getService() {
                    return domainService;
                }

                @Override
                protected PaymentRecord fromGrpc(InputCsvFileProcessingSvc.PaymentRecord grpcIn) {
                    return paymentRecordMapper.fromGrpc(grpcIn);
                }

                @Override
                protected PaymentsProcessingSvc.AckPaymentSent toGrpc(AckPaymentSent domainOut) {
                    return ackPaymentSentMapper.toGrpc(domainOut);
                }
            };

    public void remoteProcess(InputCsvFileProcessingSvc.PaymentRecord request,
                       StreamObserver<PaymentsProcessingSvc.AckPaymentSent> responseObserver) {
        adapter.remoteProcess(request, responseObserver);
    }
}
