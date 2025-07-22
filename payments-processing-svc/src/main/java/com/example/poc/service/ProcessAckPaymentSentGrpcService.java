package com.example.poc.service;

import com.example.poc.grpc.MutinyProcessAckPaymentSentServiceGrpc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class ProcessAckPaymentSentGrpcService extends
        MutinyProcessAckPaymentSentServiceGrpc.ProcessAckPaymentSentServiceImplBase {

    @Inject
    ProcessAckPaymentSentGrpcAdapterReactive adapter;

    @Override
    public Uni<PaymentsProcessingSvc.PaymentStatus> remoteProcess(
            PaymentsProcessingSvc.AckPaymentSent request) {
        return adapter.remoteProcess(request);
    }
}
