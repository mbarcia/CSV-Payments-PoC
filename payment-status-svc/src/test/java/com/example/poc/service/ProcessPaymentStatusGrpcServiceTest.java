package com.example.poc.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.mapper.PaymentOutputMapper;
import com.example.poc.common.mapper.PaymentStatusMapper;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessPaymentStatusGrpcServiceTest {

  @Mock ProcessPaymentStatusReactiveService domainService;

  @Mock PaymentStatusMapper paymentStatusMapper;

  @Mock PaymentOutputMapper paymentOutputMapper;

  @InjectMocks ProcessPaymentStatusGrpcService processPaymentStatusGrpcService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void remoteProcess() {
    // Given
    PaymentsProcessingSvc.PaymentStatus grpcRequest =
        PaymentsProcessingSvc.PaymentStatus.newBuilder().build();
    PaymentStatus domainPaymentStatus = new PaymentStatus();
    PaymentOutput domainPaymentOutput =
        new PaymentOutput(null, null, null, null, null, null, null, null, null);
    com.example.poc.grpc.PaymentStatusSvc.PaymentOutput grpcOutput =
        com.example.poc.grpc.PaymentStatusSvc.PaymentOutput.newBuilder().build();

    when(paymentStatusMapper.fromGrpc(any(PaymentsProcessingSvc.PaymentStatus.class)))
        .thenReturn(domainPaymentStatus);
    when(domainService.process(any(PaymentStatus.class)))
        .thenReturn(Uni.createFrom().item(domainPaymentOutput));
    when(paymentOutputMapper.toGrpc(any(PaymentOutput.class))).thenReturn(grpcOutput);

    // When
    Uni<com.example.poc.grpc.PaymentStatusSvc.PaymentOutput> resultUni =
        processPaymentStatusGrpcService.remoteProcess(grpcRequest);

    // Then
    UniAssertSubscriber<com.example.poc.grpc.PaymentStatusSvc.PaymentOutput> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem().assertItem(grpcOutput);
  }
}
