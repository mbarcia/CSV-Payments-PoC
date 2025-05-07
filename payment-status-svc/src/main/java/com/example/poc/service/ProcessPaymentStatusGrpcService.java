package com.example.poc.service;

import com.example.poc.domain.*;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import com.example.poc.grpc.ProcessPaymentStatusServiceGrpc;
import com.example.poc.mapper.PaymentOutputMapper;
import com.example.poc.mapper.PaymentStatusMapper;
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

    private final GrpcServiceAdapter<PaymentsProcessingSvc.PaymentStatus,
            PaymentStatusSvc.PaymentOutput,
            PaymentStatus,
            PaymentOutput> adapter =
            new GrpcServiceAdapter<>() {

                protected ProcessPaymentStatusService getService() {
                    return domainService;
                }

                @Override
                protected PaymentStatus fromGrpc(PaymentsProcessingSvc.PaymentStatus grpcIn) {
                    return paymentStatusMapper.fromGrpc(grpcIn);
                }

                @Override
                protected PaymentStatusSvc.PaymentOutput toGrpc(PaymentOutput domainOut) {
                    return paymentOutputMapper.toGrpc(domainOut);
                }
            };

    public void remoteProcess(PaymentsProcessingSvc.PaymentStatus request,
                       StreamObserver<PaymentStatusSvc.PaymentOutput> responseObserver) {
        adapter.remoteProcess(request, responseObserver);
    }
}
