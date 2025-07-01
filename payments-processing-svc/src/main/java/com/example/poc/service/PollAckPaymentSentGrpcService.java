package com.example.poc.service;

import com.example.poc.grpc.MutinyPollAckPaymentSentServiceGrpc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PollAckPaymentSentGrpcService extends
        MutinyPollAckPaymentSentServiceGrpc.PollAckPaymentSentServiceImplBase {

    @Inject
    PollAckPaymentSentGrpcAdapter adapter;

    @Override
    @Blocking
    public Uni<PaymentsProcessingSvc.PaymentStatus> remoteProcess(
            PaymentsProcessingSvc.AckPaymentSent request) {
        return adapter.remoteProcess(request);
    }
}
