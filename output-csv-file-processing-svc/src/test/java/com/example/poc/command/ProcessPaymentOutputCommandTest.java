package com.example.poc.command;

import com.example.poc.common.domain.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

class ProcessPaymentOutputCommandTest {

    @TempDir
    static File tempFile;

    @TempDir
    static File tempOutputFile;

    @InjectMocks
    ProcessPaymentOutputCommand command;

    CsvPaymentsInputFile csvPaymentsInputFile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command.setExecutor(Runnable::run);
    }

    @AfterEach
    void tearDown() throws IOException{
        Files.deleteIfExists(tempFile.toPath());
    }

    @Test
    void execute_happy() throws IOException {
        // Given
        Multi<PaymentOutput> paymentOutputList = getMultiPaymentOutput();
        ProcessPaymentOutputCommand spyCommand = spy(command);
        CsvPaymentsOutputFile csvPaymentsOutputFile = new CsvPaymentsOutputFile(tempOutputFile.getPath());

        // When (mock the getCsvPaymentsOutputFile method within the command)
        doReturn(csvPaymentsOutputFile).when(spyCommand).getCsvPaymentsOutputFile(any());

        Uni<CsvPaymentsOutputFile> resultUni = spyCommand.execute(paymentOutputList);

        // Then
        UniAssertSubscriber<CsvPaymentsOutputFile> subscriber = resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitItem().assertItem(csvPaymentsOutputFile);

        // Inspect/verify the output file
        // When: Read the first two lines
        List<String> lines = Files.readAllLines(Path.of(csvPaymentsOutputFile.getFilepath()));

        // Then: Assert header and first record
        assertThat(lines).hasSizeGreaterThanOrEqualTo(2);

        String headerLine = lines.get(0);
        String firstRecordLine = lines.get(1);
        String secondRecordLine = lines.get(2);

        assertThat(headerLine).contains("AMOUNT", "CSV ID", "CURRENCY", "FEE", "MESSAGE", "RECIPIENT", "REFERENCE", "STATUS");
        assertThat(firstRecordLine).contains("100.00", "80e055c9-7dbe-4ef0-ad37-8360eb8d1e3e", "USD", "", "", "recipient123", "abacd5c7-2230-4a24-a665-32a542468ea5", "");
        assertThat(secondRecordLine).contains("450.01", "2d8acc5b-8dae-4240-b37c-893318aba63f", "GBP", "", "", "234recipient", "746ab623-c070-49dd-87fb-ed2f39f2f3cf", "");
    }

    private Multi<PaymentOutput> getMultiPaymentOutput() {
        // Given
        csvPaymentsInputFile = new CsvPaymentsInputFile(tempFile);
        PaymentRecord paymentRecord = new PaymentRecord(
                "80e055c9-7dbe-4ef0-ad37-8360eb8d1e3e",
                "recipient123",
                new BigDecimal("100.00"),
                Currency.getInstance("USD")
        );
        paymentRecord.assignInputFile(csvPaymentsInputFile);
        PaymentOutput paymentOutput = getPaymentOutput(paymentRecord);
        paymentOutput.setConversationId(UUID.fromString("abacd5c7-2230-4a24-a665-32a542468ea5"));

        PaymentRecord paymentRecord2 = new PaymentRecord(
                "2d8acc5b-8dae-4240-b37c-893318aba63f",
                "234recipient",
                new BigDecimal("450.01"),
                Currency.getInstance("GBP")
        );
        paymentRecord2.assignInputFile(csvPaymentsInputFile);
        PaymentOutput paymentOutput2 = getPaymentOutput(paymentRecord2);
        paymentOutput2.setConversationId(UUID.fromString("746ab623-c070-49dd-87fb-ed2f39f2f3cf"));

        return Multi.createFrom().items(paymentOutput, paymentOutput2);
    }

    private PaymentOutput getPaymentOutput(PaymentRecord paymentRecord) {
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        ackPaymentSent.setPaymentRecord(paymentRecord);
        ackPaymentSent.setPaymentRecordId(paymentRecord.getId());

        PaymentStatus paymentStatus = new PaymentStatus();
        paymentStatus.setAckPaymentSent(ackPaymentSent);
        paymentStatus.setAckPaymentSentId(ackPaymentSent.getId());

        return new PaymentOutput(
                paymentStatus,
                paymentRecord.getCsvId(),
                paymentRecord.getRecipient(),
                paymentRecord.getAmount(),
                paymentRecord.getCurrency(),
                ackPaymentSent.getConversationId(),
                ackPaymentSent.getStatus(),
                paymentStatus.getMessage(),
                paymentStatus.getFee()
        );
    }

    @Test
    void execute_unhappy() throws IOException {
        // Given
        Multi<PaymentOutput> paymentOutputList = getMultiPaymentOutput();
        ProcessPaymentOutputCommand spyCommand = spy(command);

        // When (mock the getCsvPaymentsOutputFile method within the command)
        doThrow(new IOException("Test exception")).when(spyCommand).getCsvPaymentsOutputFile(any());

        Uni<CsvPaymentsOutputFile> resultUni = spyCommand.execute(paymentOutputList);

        // Then
        UniAssertSubscriber<CsvPaymentsOutputFile> subscriber = resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.awaitFailure().assertFailedWith(RuntimeException.class, "Failed to write output file.");
    }
}