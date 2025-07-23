package com.example.poc.service;

import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.mapper.PaymentOutputMapper;
import com.example.poc.common.mapper.PaymentStatusMapper;
import com.example.poc.common.service.GrpcReactiveServiceAdapter;
import com.example.poc.grpc.MutinyProcessPaymentStatusServiceGrpc;
import com.example.poc.grpc.PaymentStatusSvc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class ProcessPaymentStatusGrpcService
    extends MutinyProcessPaymentStatusServiceGrpc.ProcessPaymentStatusServiceImplBase {

  @Inject ProcessPaymentStatusReactiveService domainService;

  @Inject PaymentStatusMapper paymentStatusMapper;

  @Inject PaymentOutputMapper paymentOutputMapper;

  private final GrpcReactiveServiceAdapter<
          PaymentsProcessingSvc.PaymentStatus,
          PaymentStatusSvc.PaymentOutput,
          PaymentStatus,
          PaymentOutput>
      adapter =
          new GrpcReactiveServiceAdapter<>() {

            @Override
            protected ProcessPaymentStatusReactiveService getService() {
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

  @Override
  public Uni<PaymentStatusSvc.PaymentOutput> remoteProcess(
      PaymentsProcessingSvc.PaymentStatus request) {
    return adapter.remoteProcess(request);
  }
}
