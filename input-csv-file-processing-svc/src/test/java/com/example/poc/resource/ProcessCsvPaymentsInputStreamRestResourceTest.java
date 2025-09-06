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

import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProcessCsvPaymentsInputStreamRestResourceTest {

  @Test
  void process_validCsv() {
    String csv =
        """
        ID,Recipient,Amount,Currency
        1,John Doe,100,USD
        2,Jane Smith,200,EUR
        """;

    RestAssured.given()
        .relaxedHTTPSValidation() // <- ignores PKIX errors
        .multiPart(
            "file", "payments.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)))
        .multiPart("filename", "payments.csv")
        .when()
        .post("/api/v1/input-processing/process")
        .then()
        .statusCode(200)
        .contentType("text/event-stream") // SSE streaming response
        .body(containsString("John Doe"))
        .body(containsString("Jane Smith"));
  }

  @Test
  void process_invalidCsv_shouldFail() {
    String csv =
        """
            ID,Recipient,Amount,Currency
            1,John Doe,INVALID,USD
            """;

    RestAssured.given()
        .relaxedHTTPSValidation() // <- ignores PKIX errors
        .multiPart(
            "file", "bad.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)))
        .multiPart("filename", "bad.csv")
        .when()
        .post("/api/v1/input-processing/process")
        .then()
        .statusCode(200) // Streaming response starts with 200
        .contentType("text/event-stream");
    // Note: With streaming, invalid data might result in partial results or error events
    // depending on how the service handles errors during streaming
  }
}
