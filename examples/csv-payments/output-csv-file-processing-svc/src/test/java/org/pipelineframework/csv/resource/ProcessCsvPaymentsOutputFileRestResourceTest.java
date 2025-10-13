/*
 * Copyright (c) 2023-2025 Mariano Barcia
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

package org.pipelineframework.csv.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProcessCsvPaymentsOutputFileRestResourceTest {

    @BeforeAll
    static void setUp() {
        // Configure RestAssured to use HTTPS and trust all certificates for testing
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config =
                RestAssured.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
        // Update the port to match the HTTPS port
        RestAssured.port = 8444;
    }

    @Test
    void testProcessToFile() {
        // Create test data
        String requestBody =
                """
                [
                  {
                    "id": "%s",
                    "csvId": "CSV001",
                    "recipient": "123456789",
                    "amount": 100.00,
                    "currency": "USD",
                    "conversationId": "%s",
                    "status": 200,
                    "message": "Success",
                    "fee": 2.50,
                    "paymentStatus": {
                      "id": "%s",
                      "customerReference": null,
                      "reference": "REF001",
                      "status": "SUCCESS",
                      "message": "Payment processed successfully",
                      "fee": 2.50,
                      "ackPaymentSent": {
                        "id": "%s",
                        "conversationId": "%s",
                        "status": 200,
                        "message": "Success",
                        "paymentRecord": {
                          "id": "%s",
                          "csvId": "CSV001",
                          "recipient": "123456789",
                          "amount": 100.00,
                          "currency": "USD",
                          "csvPaymentsInputFilePath": "file:///tmp/test.csv"
                        },
                        "paymentRecordId": "%s"
                      },
                      "ackPaymentSentId": "%s",
                      "paymentRecord": {
                        "id": "%s",
                        "csvId": "CSV001",
                        "recipient": "123456789",
                        "amount": 100.00,
                        "currency": "USD",
                        "csvPaymentsInputFilePath": "file:///tmp/test.csv"
                      },
                      "paymentRecordId": "%s"
                    }
                  }
                ]
                """
                        .formatted(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/output-processing/process-file")
                .then()
                .statusCode(200)
                .body("filepath", notNullValue())
                .body("message", notNullValue());
    }

    @Test
    void testProcessToFileWithError() {
        // Create test data with an intentionally malformed object to trigger error handling
        String requestBody =
                """
                [
                  {
                    "id": "invalid-uuid",
                    "csvId": "CSV001",
                    "recipient": "123456789",
                    "amount": 100.00,
                    "currency": "USD",
                    "conversationId": "invalid-uuid",
                    "status": 200,
                    "message": "Success",
                    "fee": 2.50
                  }
                ]
                """;

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/output-processing/process-file")
                .then()
                .statusCode(400); // Jackson deserialization error results in 400
    }
}
