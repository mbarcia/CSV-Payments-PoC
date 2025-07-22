package com.example.poc.service;

import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentStatusMapper;
import com.example.poc.common.mapper.SendPaymentRequestMapper;
import com.example.poc.grpc.MutinyPaymentProviderServiceGrpc;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcService;
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

    @Override
    public Uni<PaymentsProcessingSvc.AckPaymentSent> sendPayment(PaymentStatusSvc.SendPaymentRequest grpcRequest) {
        return Uni.createFrom().item(() -> {
            var domainIn = sendPaymentRequestMapper.fromGrpc(grpcRequest);
            var domainOut = domainService.sendPayment(domainIn);
            return ackPaymentSentMapper.toGrpc(domainOut);
        }).onFailure().transform(throwable -> {
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("details", Metadata.ASCII_STRING_MARSHALLER),
                    "Error in gRPC server: " + throwable.getMessage());
            return new StatusRuntimeException(Status.INTERNAL.withDescription(throwable.getMessage()).withCause(throwable), metadata);
        });
    }

    @Override
    public Uni<PaymentsProcessingSvc.PaymentStatus> getPaymentStatus(PaymentsProcessingSvc.AckPaymentSent grpcRequest) {
        return Uni.createFrom().emitter(emitter -> {
            try {
                var domainIn = ackPaymentSentMapper.fromGrpc(grpcRequest);
                var domainOut = domainService.getPaymentStatus(domainIn);
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
