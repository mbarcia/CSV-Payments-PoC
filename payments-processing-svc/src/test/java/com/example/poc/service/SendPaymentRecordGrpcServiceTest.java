package com.example.poc.service;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class SendPaymentRecordGrpcServiceTest {

    @Mock
    private SendPaymentRecordReactiveService domainService;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private AckPaymentSentMapper ackPaymentSentMapper;

    @InjectMocks
    private SendPaymentRecordGrpcService sendPaymentRecordGrpcService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("remoteProcess: Should successfully process request and return AckPaymentSent")
    void remoteProcess_happyPath() {
        // Given
        InputCsvFileProcessingSvc.PaymentRecord grpcRequest = InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
                .setCsvId("csv1")
                .setRecipient("John Doe")
                .setAmount("100.00")
                .setCurrency("USD")
                .build();

        PaymentRecord domainIn = new PaymentRecord("csv1", "John Doe", new BigDecimal("100.00"), java.util.Currency.getInstance("USD"));
        domainIn.setId(UUID.randomUUID());

        AckPaymentSent domainOut = new AckPaymentSent();
        domainOut.setPaymentRecordId(domainIn.getId());
        domainOut.setStatus(1000L);
        domainOut.setMessage("OK");

        PaymentsProcessingSvc.AckPaymentSent grpcResponse = PaymentsProcessingSvc.AckPaymentSent.newBuilder()
                .setConversationId("conv1")
                .setPaymentRecordId(domainIn.getId().toString())
                .setStatus(1000L)
                .setMessage("OK")
                .build();

        when(paymentRecordMapper.fromGrpc(grpcRequest)).thenReturn(domainIn);
        when(domainService.process(domainIn)).thenReturn(Uni.createFrom().item(domainOut));
        when(ackPaymentSentMapper.toGrpc(domainOut)).thenReturn(grpcResponse);

        // When
        Uni<PaymentsProcessingSvc.AckPaymentSent> resultUni = sendPaymentRecordGrpcService.remoteProcess(grpcRequest);

        // Then
        PaymentsProcessingSvc.AckPaymentSent actualResponse = resultUni.await().indefinitely();
        assertThat(actualResponse).isEqualTo(grpcResponse);
    }

    @Test
    @DisplayName("remoteProcess: Should throw StatusRuntimeException on domain service error")
    void remoteProcess_domainServiceError_shouldThrowStatusRuntimeException() {
        // Given
        InputCsvFileProcessingSvc.PaymentRecord grpcRequest = InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
                .setCsvId("csv1")
                .setRecipient("John Doe")
                .setAmount("100.00")
                .setCurrency("USD")
                .build();

        PaymentRecord domainIn = new PaymentRecord("csv1", "John Doe", new BigDecimal("100.00"), java.util.Currency.getInstance("USD"));
        domainIn.setId(UUID.randomUUID());

        RuntimeException domainException = new RuntimeException("Domain service failed");

        when(paymentRecordMapper.fromGrpc(grpcRequest)).thenReturn(domainIn);
        when(domainService.process(domainIn)).thenReturn(Uni.createFrom().failure(domainException));

        // When & Then
        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () -> sendPaymentRecordGrpcService.remoteProcess(grpcRequest).await().indefinitely());
        assertThat(thrown.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(thrown.getStatus().getDescription()).isEqualTo(domainException.getMessage());
        assertThat(thrown.getStatus().getCause()).isEqualTo(domainException);
    }

    @Test
    @DisplayName("remoteProcess: Should throw RuntimeException on fromGrpc mapper error")
    void remoteProcess_fromGrpcMapperError_shouldThrowRuntimeException() {
        // Given
        InputCsvFileProcessingSvc.PaymentRecord grpcRequest = InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
                .setCsvId("csv1")
                .setRecipient("John Doe")
                .setAmount("100.00")
                .setCurrency("USD")
                .build();

        RuntimeException mapperException = new RuntimeException("Mapper failed");

        when(paymentRecordMapper.fromGrpc(grpcRequest)).thenThrow(mapperException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> sendPaymentRecordGrpcService.remoteProcess(grpcRequest).await().indefinitely());
        assertThat(thrown).isEqualTo(mapperException);
    }

    @Test
    @DisplayName("remoteProcess: Should throw StatusRuntimeException on toGrpc mapper error")
    void remoteProcess_toGrpcMapperError_shouldThrowStatusRuntimeException() {
        // Given
        InputCsvFileProcessingSvc.PaymentRecord grpcRequest = InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
                .setCsvId("csv1")
                .setRecipient("John Doe")
                .setAmount("100.00")
                .setCurrency("USD")
                .build();

        PaymentRecord domainIn = new PaymentRecord("csv1", "John Doe", new BigDecimal("100.00"), java.util.Currency.getInstance("USD"));
        domainIn.setId(UUID.randomUUID());

        AckPaymentSent domainOut = new AckPaymentSent();
        domainOut.setPaymentRecordId(domainIn.getId());
        domainOut.setStatus(1000L);
        domainOut.setMessage("OK");

        RuntimeException mapperException = new RuntimeException("Mapper failed");

        when(paymentRecordMapper.fromGrpc(grpcRequest)).thenReturn(domainIn);
        when(domainService.process(domainIn)).thenReturn(Uni.createFrom().item(domainOut));
        when(ackPaymentSentMapper.toGrpc(domainOut)).thenThrow(mapperException);

        // When & Then
        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () -> sendPaymentRecordGrpcService.remoteProcess(grpcRequest).await().indefinitely());
        assertThat(thrown.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(thrown.getStatus().getDescription()).isEqualTo(mapperException.getMessage());
        assertThat(thrown.getStatus().getCause()).isEqualTo(mapperException);
    }
}
