package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.grpc.PaymentsProcessingSvc;
import com.example.poc.grpc.PollAckPaymentSentServiceGrpc;
import com.example.poc.mappers.AckPaymentSentMapper;
import com.example.poc.mappers.PaymentStatusMapper;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
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

    public void invoke(PaymentsProcessingSvc.AckPaymentSent request,
                        StreamObserver<PaymentsProcessingSvc.PaymentStatus> responseObserver) {
        adapter.invoke(request, responseObserver);
    }
}
