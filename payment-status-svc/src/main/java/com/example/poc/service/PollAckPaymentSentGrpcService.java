package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.AckPaymentSentMapper;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.domain.PaymentStatusMapper;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.PollAckPaymentSentServiceGrpc;
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

    private final GrpcServiceAdapter<PaymentStatusSvc.AckPaymentSent,
            PaymentStatusSvc.PaymentStatus,
            AckPaymentSent,
            PaymentStatus> adapter =
            new GrpcServiceAdapter<>() {

                protected PollAckPaymentSentService getService() {
                    return domainService;
                }

                @Override
                protected AckPaymentSent fromGrpc(PaymentStatusSvc.AckPaymentSent grpcIn) {
                    return ackPaymentSentMapper.fromGrpc(grpcIn);
                }

                @Override
                protected PaymentStatusSvc.PaymentStatus toGrpc(PaymentStatus domainOut) {
                    return paymentStatusMapper.toGrpc(domainOut);
                }
            };

    public void invoke(PaymentStatusSvc.AckPaymentSent request,
                        StreamObserver<PaymentStatusSvc.PaymentStatus> responseObserver) {
        adapter.invoke(request, responseObserver);
    }
}
