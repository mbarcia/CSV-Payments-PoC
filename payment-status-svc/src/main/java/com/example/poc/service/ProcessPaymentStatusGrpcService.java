package com.example.poc.service;

import com.example.poc.domain.*;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.ProcessPaymentStatusServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

@GrpcService
public class ProcessPaymentStatusGrpcService extends ProcessPaymentStatusServiceGrpc.ProcessPaymentStatusServiceImplBase {

    @Inject
    ProcessPaymentStatusService domainService;

    @Inject
    PaymentStatusMapper paymentStatusMapper;

    @Inject
    PaymentOutputMapper paymentOutputMapper;

    private final GrpcServiceAdapter<PaymentStatusSvc.PaymentStatus,
            PaymentStatusSvc.PaymentOutput,
            PaymentStatus,
            PaymentOutput> adapter =
            new GrpcServiceAdapter<>() {

                protected ProcessPaymentStatusService getService() {
                    return domainService;
                }

                @Override
                protected PaymentStatus fromGrpc(PaymentStatusSvc.PaymentStatus grpcIn) {
                    return paymentStatusMapper.fromGrpc(grpcIn);
                }

                @Override
                protected PaymentStatusSvc.PaymentOutput toGrpc(PaymentOutput domainOut) {
                    return paymentOutputMapper.toGrpc(domainOut);
                }
            };

    public void invoke(PaymentStatusSvc.PaymentStatus request,
                       StreamObserver<PaymentStatusSvc.PaymentOutput> responseObserver) {
        adapter.invoke(request, responseObserver);
    }
}
