package com.example.poc.service;

import com.example.poc.command.ProcessPaymentOutputCommand;
import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.domain.PaymentOutput;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ProcessCsvPaymentsOutputFileReactiveServiceTest {

    @InjectMocks
    ProcessCsvPaymentsOutputFileReactiveService service;

    @Mock
    ProcessPaymentOutputCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void process() throws IOException {
        // Given
        PaymentOutput paymentOutput = new PaymentOutput(
                new com.example.poc.common.domain.PaymentStatus(),
                UUID.randomUUID().toString(),
                "John Doe",
                new BigDecimal("100.00"),
                Currency.getInstance("USD"),
                UUID.randomUUID(),
                1L,
                "Success",
                new BigDecimal("1.50")
        );
        Multi<PaymentOutput> paymentOutputMulti = Multi.createFrom().item(paymentOutput);

        CsvPaymentsOutputFile expectedOutputFile = new CsvPaymentsOutputFile("/tmp/output.csv");

        // When
        when(command.execute(any(Multi.class)))
                .thenReturn(Uni.createFrom().item(expectedOutputFile));

        Uni<CsvPaymentsOutputFile> resultUni = service.process(paymentOutputMulti);

        // Then
        UniAssertSubscriber<CsvPaymentsOutputFile> subscriber = resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem().assertItem(expectedOutputFile);
    }
}
