package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.grpc.PaymentsProcessingSvc;
import com.example.poc.grpc.ProcessAckPaymentSentServiceGrpc;
import com.example.poc.mapper.AckPaymentSentMapper;
import com.example.poc.mapper.PaymentStatusMapper;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

@GrpcService
public class ProcessAckPaymentSentGrpcService extends ProcessAckPaymentSentServiceGrpc.ProcessAckPaymentSentServiceImplBase {

    @Inject
    ProcessAckPaymentSentService domainService;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    @Inject
    PaymentStatusMapper paymentStatusMapper;

    private final GrpcServiceAdapter<PaymentsProcessingSvc.AckPaymentSent,
            PaymentsProcessingSvc.PaymentStatus,
            AckPaymentSent,
            PaymentStatus> adapter =
            new GrpcServiceAdapter<>() {

                protected ProcessAckPaymentSentService getService() {
                    return domainService;
                }

                @Override
                protected AckPaymentSent fromGrpc(PaymentsProcessingSvc.AckPaymentSent grpcIn) {
                    return ackPaymentSentMapper.fromGrpc(grpcIn);
                }

                @Override
                protected PaymentsProcessingSvc.PaymentStatus toGrpc(PaymentStatus domainOut) {
                    return paymentStatusMapper.toGrpc(domainOut);
                }
            };

    public void remoteProcess(PaymentsProcessingSvc.AckPaymentSent request,
                       StreamObserver<PaymentsProcessingSvc.PaymentStatus> responseObserver) {
        adapter.remoteProcess(request, responseObserver);
    }
}
