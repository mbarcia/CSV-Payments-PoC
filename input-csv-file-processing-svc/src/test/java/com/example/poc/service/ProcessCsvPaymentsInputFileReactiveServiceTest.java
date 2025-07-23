package com.example.poc.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.poc.command.ProcessCsvPaymentsInputFileCommand;
import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.domain.PaymentRecord;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessCsvPaymentsInputFileReactiveServiceTest {

  @InjectMocks ProcessCsvPaymentsInputFileReactiveService service;

  @Mock ProcessCsvPaymentsInputFileCommand command;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void process() {
    // Given
    CsvPaymentsInputFile inputFile = new CsvPaymentsInputFile(new java.io.File("/tmp/test.csv"));
    PaymentRecord paymentRecord =
        new PaymentRecord(
            UUID.randomUUID().toString(),
            "John Doe",
            new java.math.BigDecimal("100.00"),
            java.util.Currency.getInstance("USD"));

    when(command.execute(any(CsvPaymentsInputFile.class)))
        .thenReturn(Multi.createFrom().item(paymentRecord));

    // When
    Multi<PaymentRecord> resultMulti = service.process(inputFile);

    // Then
    AssertSubscriber<PaymentRecord> subscriber =
        resultMulti.subscribe().withSubscriber(AssertSubscriber.create(1));
    subscriber.awaitItems(1);
    subscriber.assertCompleted();
    subscriber.assertItems(paymentRecord);
  }
}
