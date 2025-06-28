package com.example.poc.service;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.service.GrpcServiceAdapter;
import com.example.poc.grpc.PaymentsProcessingSvc;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentStatusMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PollAckPaymentSentGrpcAdapter extends GrpcServiceAdapter<
        PaymentsProcessingSvc.AckPaymentSent,
        PaymentsProcessingSvc.PaymentStatus,
        AckPaymentSent,
        PaymentStatus> {

    @Inject
    PollAckPaymentSentService domainService;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    @Inject
    PaymentStatusMapper paymentStatusMapper;

    @Override
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
}
