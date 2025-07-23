package com.example.poc.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessCsvPaymentsInputFileGrpcServiceTest {

  @Mock ProcessCsvPaymentsInputFileReactiveService domainService;

  @Mock CsvPaymentsInputFileMapper csvPaymentsInputFileMapper;

  @Mock PaymentRecordMapper paymentRecordMapper;

  @InjectMocks ProcessCsvPaymentsInputFileGrpcService grpcService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void remoteProcess() {
    // Given
    InputCsvFileProcessingSvc.CsvPaymentsInputFile grpcRequest =
        InputCsvFileProcessingSvc.CsvPaymentsInputFile.newBuilder()
            .setFilepath("/tmp/test.csv")
            .build();

    InputCsvFileProcessingSvc.PaymentRecord grpcOutput =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder().build();

    CsvPaymentsInputFile domainInputFile =
        new CsvPaymentsInputFile(new java.io.File("/tmp/test.csv"));

    PaymentRecord domainPaymentRecord =
        new PaymentRecord(
            UUID.randomUUID().toString(),
            "John Doe",
            new BigDecimal("100.00"),
            Currency.getInstance("USD"));

    // When
    when(csvPaymentsInputFileMapper.fromGrpc(
            any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class)))
        .thenReturn(domainInputFile);
    when(domainService.process(any(CsvPaymentsInputFile.class)))
        .thenReturn(Multi.createFrom().item(domainPaymentRecord));
    when(paymentRecordMapper.toGrpc(any(PaymentRecord.class))).thenReturn(grpcOutput);

    InputCsvFileProcessingSvc.PaymentRecord grpcPaymentRecord =
        InputCsvFileProcessingSvc.PaymentRecord.newBuilder()
            .setCsvId(domainPaymentRecord.getCsvId())
            .setRecipient(domainPaymentRecord.getRecipient())
            .setAmount(domainPaymentRecord.getAmount().toPlainString())
            .setCurrency(domainPaymentRecord.getCurrency().getCurrencyCode())
            .build();

    when(csvPaymentsInputFileMapper.fromGrpc(
            any(InputCsvFileProcessingSvc.CsvPaymentsInputFile.class)))
        .thenReturn(domainInputFile);
    when(domainService.process(any(CsvPaymentsInputFile.class)))
        .thenReturn(Multi.createFrom().item(domainPaymentRecord));
    when(paymentRecordMapper.toDto(any(PaymentRecord.class)))
        .thenReturn(
            com.example.poc.common.dto.PaymentRecordDto.builder()
                .id(domainPaymentRecord.getId())
                .csvId(domainPaymentRecord.getCsvId())
                .recipient(domainPaymentRecord.getRecipient())
                .amount(domainPaymentRecord.getAmount())
                .currency(domainPaymentRecord.getCurrency())
                .csvPaymentsInputFilePath(domainInputFile.getFilepath())
                .build()); // Mock the DTO conversion
    when(paymentRecordMapper.toGrpc(any(com.example.poc.common.dto.PaymentRecordDto.class)))
        .thenReturn(grpcPaymentRecord);

    Multi<InputCsvFileProcessingSvc.PaymentRecord> resultMulti =
        grpcService.remoteProcess(grpcRequest);

    // Then
    AssertSubscriber<InputCsvFileProcessingSvc.PaymentRecord> subscriber =
        resultMulti.subscribe().withSubscriber(AssertSubscriber.create(1));
    subscriber.awaitItems(1);
    subscriber.assertCompleted();
    subscriber.assertItems(grpcPaymentRecord);
  }
}
