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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.example.poc.common.domain.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProcessCsvPaymentsOutputFileReactiveServiceTest {

  ProcessCsvPaymentsOutputFileReactiveService service;

  @TempDir static File tempFile;

  CsvPaymentsInputFile csvPaymentsInputFile;

  @BeforeEach
  void setUp() {
    service = new ProcessCsvPaymentsOutputFileReactiveService(Runnable::run);
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.deleteIfExists(tempFile.toPath());
  }

  @Test
  void execute_happy() throws IOException {
    // Given
    Multi<PaymentOutput> paymentOutputList = getMultiPaymentOutput();

    Uni<CsvPaymentsOutputFile> resultUni = service.process(paymentOutputList);

    // Inspect/verify the output file
    // When: Read the first two lines
    List<String> lines =
        Files.readAllLines(Path.of(resultUni.await().indefinitely().getFilepath()));

    // Then: Assert header and first record
    assertThat(lines).hasSizeGreaterThanOrEqualTo(2);

    String headerLine = lines.get(0);
    String firstRecordLine = lines.get(1);
    String secondRecordLine = lines.get(2);

    AssertionsForClassTypes.assertThat(headerLine)
        .contains(
            "AMOUNT", "CSV ID", "CURRENCY", "FEE", "MESSAGE", "RECIPIENT", "REFERENCE", "STATUS");
    AssertionsForClassTypes.assertThat(firstRecordLine)
        .contains(
            "100.00",
            "80e055c9-7dbe-4ef0-ad37-8360eb8d1e3e",
            "USD",
            "",
            "",
            "recipient123",
            "abacd5c7-2230-4a24-a665-32a542468ea5",
            "");
    AssertionsForClassTypes.assertThat(secondRecordLine)
        .contains(
            "450.01",
            "2d8acc5b-8dae-4240-b37c-893318aba63f",
            "GBP",
            "",
            "",
            "234recipient",
            "746ab623-c070-49dd-87fb-ed2f39f2f3cf",
            "");
  }

  private Multi<PaymentOutput> getMultiPaymentOutput() {
    // Given
    csvPaymentsInputFile = new CsvPaymentsInputFile(tempFile);
    PaymentRecord paymentRecord =
        new PaymentRecord(
            "80e055c9-7dbe-4ef0-ad37-8360eb8d1e3e",
            "recipient123",
            new BigDecimal("100.00"),
            Currency.getInstance("USD"));
    paymentRecord.assignInputFile(csvPaymentsInputFile);
    PaymentOutput paymentOutput = getPaymentOutput(paymentRecord);
    paymentOutput.setConversationId(UUID.fromString("abacd5c7-2230-4a24-a665-32a542468ea5"));

    PaymentRecord paymentRecord2 =
        new PaymentRecord(
            "2d8acc5b-8dae-4240-b37c-893318aba63f",
            "234recipient",
            new BigDecimal("450.01"),
            Currency.getInstance("GBP"));
    paymentRecord2.assignInputFile(csvPaymentsInputFile);
    PaymentOutput paymentOutput2 = getPaymentOutput(paymentRecord2);
    paymentOutput2.setConversationId(UUID.fromString("746ab623-c070-49dd-87fb-ed2f39f2f3cf"));

    return Multi.createFrom().items(paymentOutput, paymentOutput2);
  }

  private Multi<PaymentOutput> getBadMultiPaymentOutput() {
    // Given
    PaymentRecord paymentRecord =
        new PaymentRecord(
            "80e055c9-7dbe-4ef0-ad37-8360eb8d1e3e",
            "recipient123",
            new BigDecimal("100.00"),
            Currency.getInstance("USD"));
    paymentRecord.setCsvPaymentsInputFilePath("bad-file-path");
    PaymentOutput paymentOutput = getPaymentOutput(paymentRecord);
    paymentOutput.setConversationId(UUID.fromString("abacd5c7-2230-4a24-a665-32a542468ea5"));

    csvPaymentsInputFile = new CsvPaymentsInputFile(tempFile);
    PaymentRecord paymentRecord2 =
        new PaymentRecord(
            "2d8acc5b-8dae-4240-b37c-893318aba63f",
            "234recipient",
            new BigDecimal("450.01"),
            Currency.getInstance("GBP"));
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
        paymentStatus.getFee());
  }

  @Test
  void execute_unhappy() {
    // Given
    Multi<PaymentOutput> paymentOutputList = getBadMultiPaymentOutput();

    // When (mock the getCsvPaymentsOutputFile method within the command)

    Uni<CsvPaymentsOutputFile> resultUni = service.process(paymentOutputList);

    // Then
    UniAssertSubscriber<CsvPaymentsOutputFile> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber
        .awaitFailure()
        .assertFailedWith(RuntimeException.class, "Failed to write output file.");
  }
}
