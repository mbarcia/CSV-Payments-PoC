package com.example.poc.service;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import com.example.poc.common.service.GrpcReactiveServiceAdapter;
import com.example.poc.common.service.ReactiveService;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.MutinySendPaymentRecordServiceGrpc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class SendPaymentRecordGrpcService
    extends MutinySendPaymentRecordServiceGrpc.SendPaymentRecordServiceImplBase {

  @Inject SendPaymentRecordReactiveService domainService;

  @Inject PaymentRecordMapper paymentRecordMapper;

  @Inject AckPaymentSentMapper ackPaymentSentMapper;

  @Override
  public Uni<PaymentsProcessingSvc.AckPaymentSent> remoteProcess(
      InputCsvFileProcessingSvc.PaymentRecord request) {

    return new GrpcReactiveServiceAdapter<
        InputCsvFileProcessingSvc.PaymentRecord, // GrpcIn
        PaymentsProcessingSvc.AckPaymentSent, // GrpcOut
        PaymentRecord, // DomainIn
        AckPaymentSent>() // DomainOut
    {
      @Override
      protected ReactiveService<PaymentRecord, AckPaymentSent> getService() {
        return domainService;
      }

      @Override
      protected PaymentRecord fromGrpc(InputCsvFileProcessingSvc.PaymentRecord grpcIn) {
        return paymentRecordMapper.fromGrpc(grpcIn);
      }

      @Override
      protected PaymentsProcessingSvc.AckPaymentSent toGrpc(AckPaymentSent domainOut) {
        return ackPaymentSentMapper.toGrpc(domainOut);
      }
    }.remoteProcess(request);
  }
}
