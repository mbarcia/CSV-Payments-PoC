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

package io.github.mbarcia.csv.service;

import static io.github.mbarcia.csv.service.PaymentOutputTestBuilder.aPaymentOutput;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.github.mbarcia.csv.common.domain.AckPaymentSent;
import io.github.mbarcia.csv.common.domain.CsvPaymentsOutputFile;
import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.common.dto.PaymentOutputDto;
import io.github.mbarcia.csv.common.mapper.PaymentOutputMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
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
import org.mapstruct.factory.Mappers;

class ProcessCsvPaymentsOutputFileReactiveServiceTest {

  ProcessCsvPaymentsOutputFileReactiveService service;

  PaymentOutputMapper mapper = Mappers.getMapper(PaymentOutputMapper.class);

  @TempDir static Path tempDir;
  static Path tempFile;

  @BeforeEach
  void setUp() throws IOException {
    service = new ProcessCsvPaymentsOutputFileReactiveService(Runnable::run);
    tempFile = Files.createFile(tempDir.resolve("test.csv"));
  }

  @AfterEach
  void tearDown() throws IOException {
    if (tempFile != null && Files.exists(tempFile)) {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  void execute_happy() throws IOException {
    // Given
    Multi<PaymentOutput> paymentOutputList = getMultiPaymentOutput();

    Uni<CsvPaymentsOutputFile> resultUni = service.process(paymentOutputList);

    // Inspect/verify the output file
    // When: Read the first two lines
    List<String> lines = Files.readAllLines(resultUni.await().indefinitely().getFilepath());

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
    PaymentRecord paymentRecord = new PaymentRecord();
    paymentRecord.setCsvPaymentsInputFilePath(tempFile);
    paymentRecord.setCsvId(String.valueOf(UUID.randomUUID()));
    paymentRecord.setRecipient("John Doe");
    paymentRecord.setAmount(new BigDecimal("100.00"));
    paymentRecord.setCurrency(Currency.getInstance("USD"));

    AckPaymentSent ackPaymentSent = new AckPaymentSent();
    ackPaymentSent.setPaymentRecord(paymentRecord);
    ackPaymentSent.setConversationId(UUID.randomUUID());
    PaymentStatus paymentStatus = new PaymentStatus();
    paymentStatus.setAckPaymentSent(ackPaymentSent);
    paymentStatus.setStatus("nada");
    paymentStatus.setMessage("Success");

    PaymentOutputDto paymentOutputDto1 =
        aPaymentOutput()
            .withCsvId("80e055c9-7dbe-4ef0-ad37-8360eb8d1e3e")
            .withRecipient("recipient123")
            .withAmount(new BigDecimal("100.00"))
            .withCurrency(Currency.getInstance("USD"))
            .withConversationId(UUID.fromString("abacd5c7-2230-4a24-a665-32a542468ea5"))
            .withPaymentStatus(paymentStatus)
            .buildDto();

    PaymentOutputDto paymentOutputDto2 =
        aPaymentOutput()
            .withCsvId("2d8acc5b-8dae-4240-b37c-893318aba63f")
            .withRecipient("234recipient")
            .withAmount(new BigDecimal("450.01"))
            .withCurrency(Currency.getInstance("GBP"))
            .withConversationId(UUID.fromString("746ab623-c070-49dd-87fb-ed2f39f2f3cf"))
            .withPaymentStatus(paymentStatus)
            .buildDto();

    return Multi.createFrom()
        .items(mapper.fromDto(paymentOutputDto1), mapper.fromDto(paymentOutputDto2));
  }

  private Multi<PaymentOutput> getBadMultiPaymentOutput() {
    return Multi.createFrom().empty();
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
        .assertFailedWith(
            RuntimeException.class, "Failed to write output file after processing 0 records.");
  }
}
