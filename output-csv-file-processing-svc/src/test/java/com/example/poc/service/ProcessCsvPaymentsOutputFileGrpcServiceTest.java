package com.example.poc.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.common.mapper.PaymentOutputMapper;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import com.example.poc.grpc.PaymentStatusSvc;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("unchecked")
class ProcessCsvPaymentsOutputFileGrpcServiceTest {

  @InjectMocks ProcessCsvPaymentsOutputFileGrpcService grpcService;

  @Mock ProcessCsvPaymentsOutputFileReactiveService domainService;

  @Mock CsvPaymentsOutputFileMapper csvPaymentsOutputFileMapper;

  @Mock PaymentOutputMapper paymentOutputMapper;

  private PaymentStatusSvc.PaymentOutput grpcPaymentOutput;
  private PaymentOutput domainPaymentOutput;
  private CsvPaymentsOutputFile domainOutputFile;
  private OutputCsvFileProcessingSvc.CsvPaymentsOutputFile grpcOutputFile;

  @BeforeEach
  void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);

    grpcPaymentOutput =
        PaymentStatusSvc.PaymentOutput.newBuilder()
            .setCsvId(UUID.randomUUID().toString())
            .setRecipient("John Doe")
            .setAmount(new BigDecimal("100.00").toPlainString())
            .setCurrency(Currency.getInstance("USD").getCurrencyCode())
            .setConversationId(UUID.randomUUID().toString())
            .setStatus(1L)
            .setMessage("Success")
            .setFee(new BigDecimal("1.50").toPlainString())
            .build();

    domainPaymentOutput =
        new PaymentOutput(
            new PaymentStatus(),
            UUID.randomUUID().toString(),
            "John Doe",
            new BigDecimal("100.00"),
            Currency.getInstance("USD"),
            UUID.randomUUID(),
            1L,
            "Success",
            new BigDecimal("1.50"));

    domainOutputFile = new CsvPaymentsOutputFile("/tmp/output.csv");

    grpcOutputFile =
        OutputCsvFileProcessingSvc.CsvPaymentsOutputFile.newBuilder()
            .setFilepath("/tmp/output.csv")
            .setCsvFolderPath("/tmp/")
            .build();
  }

  @Test
  void remoteProcess() throws IOException {
    // Given
    Multi<PaymentStatusSvc.PaymentOutput> grpcStream = Multi.createFrom().item(grpcPaymentOutput);

    when(paymentOutputMapper.fromGrpc(any(PaymentStatusSvc.PaymentOutput.class)))
        .thenReturn(domainPaymentOutput);
    when(domainService.process(any(Multi.class)))
        .thenAnswer(
            invocation -> {
              Multi<PaymentOutput> input = invocation.getArgument(0);
              return input.collect().asList().onItem().transform(list -> domainOutputFile);
            });
    when(csvPaymentsOutputFileMapper.toGrpc(any(CsvPaymentsOutputFile.class)))
        .thenReturn(grpcOutputFile);

    // When
    Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> resultUni =
        grpcService.remoteProcess(grpcStream);

    // Then
    UniAssertSubscriber<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem().assertItem(grpcOutputFile);
  }

  @Test
  void testRemoteProcess_ShouldInvokeMappers() {
    // Given
    Multi<PaymentStatusSvc.PaymentOutput> grpcStream = Multi.createFrom().item(grpcPaymentOutput);

    when(paymentOutputMapper.fromGrpc(grpcPaymentOutput)).thenReturn(domainPaymentOutput);
    when(domainService.process(any(Multi.class)))
        .thenAnswer(
            invocation -> {
              Multi<PaymentOutput> input = invocation.getArgument(0);
              return input.collect().asList().onItem().transform(list -> domainOutputFile);
            });
    when(csvPaymentsOutputFileMapper.toGrpc(domainOutputFile)).thenReturn(grpcOutputFile);

    // When
    grpcService.remoteProcess(grpcStream).await().indefinitely();

    // Then
    verify(paymentOutputMapper).fromGrpc(grpcPaymentOutput);
    verify(csvPaymentsOutputFileMapper).toGrpc(domainOutputFile);
  }
}
