package com.example.poc.service;

import com.example.poc.grpc.MutinyPaymentProviderServiceGrpc;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentStatusMapper;
import com.example.poc.common.mapper.SendPaymentRequestMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PaymentProviderGrpcService extends MutinyPaymentProviderServiceGrpc.PaymentProviderServiceImplBase {

    @Inject
    PaymentProviderService domainService;

    @Inject
    SendPaymentRequestMapper sendPaymentRequestMapper;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    @Inject
    PaymentStatusMapper paymentStatusMapper;

    @Blocking
    @Override
    public Uni<PaymentsProcessingSvc.AckPaymentSent> sendPayment(PaymentStatusSvc.SendPaymentRequest grpcRequest) {
        return Uni.createFrom().item(() -> {
            var domainIn = sendPaymentRequestMapper.fromGrpc(grpcRequest);
            var domainOut = domainService.sendPayment(domainIn);
            return ackPaymentSentMapper.toGrpc(domainOut);
        });
    }

    @Blocking
    @Override
    public Uni<PaymentsProcessingSvc.PaymentStatus> getPaymentStatus(PaymentsProcessingSvc.AckPaymentSent grpcRequest) {
        return Uni.createFrom().emitter(emitter -> {
            try {
                var domainIn = ackPaymentSentMapper.fromGrpc(grpcRequest);
                var domainOut = domainService.getPaymentStatus(domainIn); // throws checked exception
                var grpcResponse = paymentStatusMapper.toGrpc(domainOut);
                emitter.complete(grpcResponse);
            } catch (Exception e) {
                emitter.fail(io.grpc.Status.INTERNAL
                        .withDescription("Processing failed: " + e.getMessage())
                        .withCause(e)
                        .asRuntimeException());
            }
        });
    }
}
