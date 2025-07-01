package com.example.poc.service;

import com.example.poc.grpc.MutinyProcessAckPaymentSentServiceGrpc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class ProcessAckPaymentSentGrpcService extends
        MutinyProcessAckPaymentSentServiceGrpc.ProcessAckPaymentSentServiceImplBase {

    @Inject
    ProcessAckPaymentSentGrpcAdapter adapter;

    @Override
    @Blocking
    public Uni<PaymentsProcessingSvc.PaymentStatus> remoteProcess(
            PaymentsProcessingSvc.AckPaymentSent request) {
        return adapter.remoteProcess(request);
    }
}
