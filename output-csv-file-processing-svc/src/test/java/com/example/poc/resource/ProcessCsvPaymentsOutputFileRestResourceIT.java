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

package com.example.poc.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.dto.PaymentOutputDto;
import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProcessCsvPaymentsOutputFileRestResourceIT {

  @Test
  void testProcessToFileEndpoint() {
    // Create a complete object structure for testing
    PaymentRecord paymentRecord = new PaymentRecord();
    paymentRecord.setCsvId("CSV001");
    paymentRecord.setRecipient("123456789");
    paymentRecord.setAmount(new BigDecimal("100.00"));
    paymentRecord.setCurrency(Currency.getInstance("USD"));
    paymentRecord.setCsvPaymentsInputFilePath(Paths.get("/tmp/test.csv"));

    AckPaymentSent ackPaymentSent = new AckPaymentSent(UUID.randomUUID());
    ackPaymentSent.setStatus(200L);
    ackPaymentSent.setMessage("Success");
    ackPaymentSent.setPaymentRecord(paymentRecord);
    ackPaymentSent.setPaymentRecordId(paymentRecord.getId());

    PaymentStatus paymentStatus = new PaymentStatus();
    paymentStatus.setReference("REF001");
    paymentStatus.setStatus("SUCCESS");
    paymentStatus.setMessage("Payment processed successfully");
    paymentStatus.setFee(new BigDecimal("2.50"));
    paymentStatus.setAckPaymentSent(ackPaymentSent);
    paymentStatus.setAckPaymentSentId(ackPaymentSent.getId());
    paymentStatus.setPaymentRecord(paymentRecord);
    paymentStatus.setPaymentRecordId(paymentRecord.getId());

    PaymentOutput paymentOutput = new PaymentOutput();
    paymentOutput.setCsvId("CSV001");
    paymentOutput.setRecipient("123456789");
    paymentOutput.setAmount(new BigDecimal("100.00"));
    paymentOutput.setCurrency(Currency.getInstance("USD"));
    paymentOutput.setConversationId(UUID.randomUUID());
    paymentOutput.setStatus(200L);
    paymentOutput.setMessage("Success");
    paymentOutput.setFee(new BigDecimal("2.50"));
    paymentOutput.setPaymentStatus(paymentStatus);

    // Create DTO from the domain object
    PaymentOutputDto dto =
        PaymentOutputDto.builder()
            .id(paymentOutput.getId())
            .csvId(paymentOutput.getCsvId())
            .recipient(paymentOutput.getRecipient())
            .amount(paymentOutput.getAmount())
            .currency(paymentOutput.getCurrency())
            .conversationId(paymentOutput.getConversationId())
            .status(paymentOutput.getStatus())
            .message(paymentOutput.getMessage())
            .fee(paymentOutput.getFee())
            .build();

    // When & Then
    given()
        .contentType("application/json")
        .body(List.of(dto))
        .when()
        .post("/api/v1/output-processing/process-file")
        .then()
        .statusCode(200)
        .body("message", containsString("processed"))
        .body("filepath", notNullValue());
  }
}
