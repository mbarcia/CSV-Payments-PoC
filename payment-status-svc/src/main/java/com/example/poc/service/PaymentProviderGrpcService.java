package com.example.poc.service;

import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentStatus;
import com.example.poc.grpc.PaymentProviderServiceGrpc;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import com.example.poc.mapper.AckPaymentSentMapper;
import com.example.poc.mapper.PaymentStatusMapper;
import com.example.poc.mapper.SendPaymentRequestMapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

@GrpcService
public class PaymentProviderGrpcService extends PaymentProviderServiceGrpc.PaymentProviderServiceImplBase {

    @Inject
    PaymentProviderService domainService;

    @Inject
    SendPaymentRequestMapper sendPaymentRequestMapper;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    @Inject
    PaymentStatusMapper paymentStatusMapper;

    public void sendPayment(PaymentStatusSvc.SendPaymentRequest grpcRequest,
                            StreamObserver<PaymentsProcessingSvc.AckPaymentSent> responseObserver) {
        try {
            SendPaymentRequestMapper.SendPaymentRequest domainIn = sendPaymentRequestMapper.fromGrpc(grpcRequest);
            AckPaymentSent domainOut = domainService.sendPayment(domainIn);
            PaymentsProcessingSvc.AckPaymentSent grpcResponse = ackPaymentSentMapper.toGrpc(domainOut);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Processing failed: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    public void getPaymentStatus(PaymentsProcessingSvc.AckPaymentSent grpcRequest, StreamObserver<PaymentsProcessingSvc.PaymentStatus> responseObserver) {
        try {
            AckPaymentSent domainIn = ackPaymentSentMapper.fromGrpc(grpcRequest);
            PaymentStatus domainOut = domainService.getPaymentStatus(domainIn);
            PaymentsProcessingSvc.PaymentStatus grpcResponse = paymentStatusMapper.toGrpc(domainOut);
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Processing failed: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
