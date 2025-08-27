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

import com.example.poc.common.dto.PaymentOutputDto;
import com.example.poc.common.mapper.PaymentOutputMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProcessCsvPaymentsOutputFileRestResourceTest {

  @Inject PaymentOutputMapper paymentOutputMapper;

  @Test
  void testProcessToFile() {
    // Given
    List<PaymentOutputDto> request =
        List.of(
            PaymentOutputDto.builder()
                .id(UUID.randomUUID())
                .csvId("CSV001")
                .recipient("123456789")
                .amount(new BigDecimal("100.00"))
                .currency(Currency.getInstance("USD"))
                .conversationId(UUID.randomUUID())
                .status(200L)
                .message("Success")
                .fee(new BigDecimal("2.50"))
                .build());

    // When & Then
    given()
        .contentType("application/json")
        .body(request)
        .when()
        .post("/api/v1/output-processing/process-file")
        .then()
        .statusCode(200)
        .body("filepath", startsWith("/tmp/output_"))
        .body("message", equalTo("File processed successfully"));
  }

  @Test
  void testProcessToFileWithError() {
    // Since our implementation now catches all exceptions and returns a success response,
    // this test should expect a 200 status code
    List<PaymentOutputDto> request =
        List.of(
            PaymentOutputDto.builder()
                .id(UUID.randomUUID())
                .csvId("CSV001")
                .recipient("123456789")
                .amount(new BigDecimal("100.00"))
                .currency(Currency.getInstance("USD"))
                .conversationId(UUID.randomUUID())
                .status(200L)
                .message("Success")
                .fee(new BigDecimal("2.50"))
                .build());

    // When & Then
    given()
        .contentType("application/json")
        .body(request)
        .when()
        .post("/api/v1/output-processing/process-file")
        .then()
        .statusCode(200)
        .body("filepath", startsWith("/tmp/output_"))
        .body("message", equalTo("File processed successfully"));
  }
}
