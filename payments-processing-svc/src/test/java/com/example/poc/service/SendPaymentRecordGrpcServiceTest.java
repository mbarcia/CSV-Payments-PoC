/*
 * Copyright © 2023-2025 Mariano Barcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.poc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.dto.PaymentRecordDto;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SendPaymentRecordGrpcServiceTest {

  @Mock private SendPaymentRecordReactiveService domainService;

  @Mock private PaymentRecordMapper paymentRecordMapper;

  @Mock private AckPaymentSentMapper ackPaymentSentMapper;

  @InjectMocks private SendPaymentRecordGrpcService sendPaymentRecordGrpcService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("remoteProcess: Should successfully process request and return AckPaymentSent")
  void remoteProcess_happyPath() {
    // Given
    UUID id = UUID.randomUUID();
    UUID csvId = UUID.randomUUID();

    InputCsvFileProcessingSvc.PaymentRecord grpcRequest =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
            .setId(String.valueOf(id))
            .setCsvId(String.valueOf(csvId))
            .setRecipient("John Doe")
            .setAmount("100.00")
            .setCurrency("USD")
            .build();

    PaymentRecordDto dtoIn =
        PaymentRecordDto.builder()
            .id(id)
            .csvId(String.valueOf(csvId))
            .recipient("John Doe")
            .amount(BigDecimal.valueOf(100.00))
            .currency(java.util.Currency.getInstance("USD"))
            .build();
    PaymentRecord domainIn = Mappers.getMapper(PaymentRecordMapper.class).fromDto(dtoIn);

    AckPaymentSent domainOut = new AckPaymentSent();
    domainOut.setPaymentRecordId(domainIn.getId());
    domainOut.setStatus(1000L);
    domainOut.setMessage("OK");

    PaymentsProcessingSvc.AckPaymentSent grpcResponse =
        PaymentsProcessingSvc.AckPaymentSent.newBuilder()
            .setConversationId("conv1")
            .setPaymentRecordId(domainIn.getId().toString())
            .setStatus(1000L)
            .setMessage("OK")
            .build();

    doReturn(domainIn).when(paymentRecordMapper).fromGrpc(grpcRequest);
    doReturn(Uni.createFrom().item(domainOut)).when(domainService).process(domainIn);
    when(ackPaymentSentMapper.toGrpc(domainOut)).thenReturn(grpcResponse);

    // When
    Uni<PaymentsProcessingSvc.AckPaymentSent> resultUni =
        sendPaymentRecordGrpcService.remoteProcess(grpcRequest);

    // Then
    PaymentsProcessingSvc.AckPaymentSent actualResponse = resultUni.await().indefinitely();
    assertThat(actualResponse).isEqualTo(grpcResponse);
  }

  @Test
  @DisplayName("remoteProcess: Should throw StatusRuntimeException on domain service error")
  void remoteProcess_domainServiceError_shouldThrowStatusRuntimeException() {
    // Given
    InputCsvFileProcessingSvc.PaymentRecord grpcRequest =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
            .setCsvId("csv1")
            .setRecipient("John Doe")
            .setAmount("100.00")
            .setCurrency("USD")
            .build();

    PaymentRecordDto dtoIn =
        PaymentRecordDto.builder()
            .id(UUID.randomUUID())
            .csvId(String.valueOf(UUID.randomUUID()))
            .recipient("John Doe")
            .amount(BigDecimal.valueOf(100.00))
            .currency(java.util.Currency.getInstance("USD"))
            .build();
    PaymentRecord domainIn = Mappers.getMapper(PaymentRecordMapper.class).fromDto(dtoIn);

    RuntimeException domainException = new RuntimeException("Domain service failed");

    doReturn(domainIn).when(paymentRecordMapper).fromGrpc(grpcRequest);
    doReturn(Uni.createFrom().failure(domainException)).when(domainService).process(domainIn);

    // When & Then
    StatusRuntimeException thrown =
        assertThrows(
            StatusRuntimeException.class,
            () -> sendPaymentRecordGrpcService.remoteProcess(grpcRequest).await().indefinitely());
    assertThat(thrown.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
    assertThat(thrown.getStatus().getDescription()).isEqualTo(domainException.getMessage());
    assertThat(thrown.getStatus().getCause()).isEqualTo(domainException);
  }

  @Test
  @DisplayName("remoteProcess: Should throw RuntimeException on fromGrpc mapper error")
  void remoteProcess_fromGrpcMapperError_shouldThrowRuntimeException() {
    // Given
    InputCsvFileProcessingSvc.PaymentRecord grpcRequest =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
            .setCsvId("csv1")
            .setRecipient("John Doe")
            .setAmount("100.00")
            .setCurrency("USD")
            .build();

    RuntimeException mapperException = new RuntimeException("Mapper failed");

    when(paymentRecordMapper.fromGrpc(grpcRequest)).thenThrow(mapperException);

    // When & Then
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> sendPaymentRecordGrpcService.remoteProcess(grpcRequest).await().indefinitely());
    assertThat(thrown).isEqualTo(mapperException);
  }

  @Test
  @DisplayName("remoteProcess: Should throw StatusRuntimeException on toGrpc mapper error")
  void remoteProcess_toGrpcMapperError_shouldThrowStatusRuntimeException() {
    // Given
    InputCsvFileProcessingSvc.PaymentRecord grpcRequest =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
            .setCsvId("csv1")
            .setRecipient("John Doe")
            .setAmount("100.00")
            .setCurrency("USD")
            .build();

    PaymentRecordDto dtoIn =
        PaymentRecordDto.builder()
            .id(UUID.randomUUID())
            .csvId(String.valueOf(UUID.randomUUID()))
            .recipient("John Doe")
            .amount(BigDecimal.valueOf(100.00))
            .currency(java.util.Currency.getInstance("USD"))
            .build();
    PaymentRecord domainIn = Mappers.getMapper(PaymentRecordMapper.class).fromDto(dtoIn);

    AckPaymentSent domainOut = new AckPaymentSent();
    domainOut.setPaymentRecordId(domainIn.getId());
    domainOut.setStatus(1000L);
    domainOut.setMessage("OK");

    RuntimeException mapperException = new RuntimeException("Mapper failed");

    doReturn(domainIn).when(paymentRecordMapper).fromGrpc(grpcRequest);
    doReturn(Uni.createFrom().item(domainOut)).when(domainService).process(domainIn);
    when(ackPaymentSentMapper.toGrpc(domainOut)).thenThrow(mapperException);

    // When & Then
    StatusRuntimeException thrown =
        assertThrows(
            StatusRuntimeException.class,
            () -> sendPaymentRecordGrpcService.remoteProcess(grpcRequest).await().indefinitely());
    assertThat(thrown.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
    assertThat(thrown.getStatus().getDescription()).isEqualTo(mapperException.getMessage());
    assertThat(thrown.getStatus().getCause()).isEqualTo(mapperException);
  }
}
