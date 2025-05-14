package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.grpc.PaymentsProcessingSvc;
import com.example.poc.grpc.PollAckPaymentSentServiceGrpc;
import com.example.poc.mapper.AckPaymentSentMapper;
import com.example.poc.mapper.PaymentStatusMapper;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;

@GrpcService
public class PollAckPaymentSentGrpcService extends PollAckPaymentSentServiceGrpc.PollAckPaymentSentServiceImplBase {

    @Inject
    PollAckPaymentSentService domainService;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    @Inject
    PaymentStatusMapper paymentStatusMapper;

    private final GrpcServiceAdapter<PaymentsProcessingSvc.AckPaymentSent,
            PaymentsProcessingSvc.PaymentStatus,
            AckPaymentSent,
            PaymentStatus> adapter =
            new GrpcServiceAdapter<>() {

                protected PollAckPaymentSentService getService() {
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

    @Blocking
    public void remoteProcess(PaymentsProcessingSvc.AckPaymentSent request,
                        StreamObserver<PaymentsProcessingSvc.PaymentStatus> responseObserver) {
        adapter.remoteProcess(request, responseObserver);
    }
}
