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

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class SendPaymentRecordResourceIT {

    @BeforeAll
    static void setUp() {
        // Configure RestAssured to use HTTPS and trust all certificates for testing
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config =
                RestAssured.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
        // Update the port to match the HTTPS port
        RestAssured.port = 8445;
    }

    @AfterAll
    static void tearDown() {
        // Reset RestAssured to default configuration
        RestAssured.reset();
    }

    @Test
    void testSendPaymentEndpointWithValidData() {
        // Create a test DTO with valid structure
        String requestBody =
                """
                {
                  "id": "%s",
                  "csvId": "CSV123",
                  "recipient": "John Doe",
                  "amount": 100.50,
                  "currency": "EUR",
                  "csvPaymentsInputFilePath": "/tmp/test.csv"
                }
                """
                        .formatted(UUID.randomUUID());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/send-payment-record/process")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("conversationId", notNullValue())
                .body("status", notNullValue());
    }

    @Test
    void testSendPaymentEndpointWithInvalidUUID() {
        // Create a test DTO with invalid UUID
        String requestBody =
                """
                {
                  "id": "invalid-uuid",
                  "csvId": "CSV123",
                  "recipient": "John Doe",
                  "amount": 100.50,
                  "currency": "EUR",
                  "csvPaymentsInputFilePath": "/tmp/test.csv"
                }
                """;

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/send-payment-record/process")
                .then()
                .statusCode(500);
    }

    @Test
    void testSendPaymentEndpointWithMissingRequiredFields() {
        // Create a test DTO with missing required fields
        String requestBody =
                """
                {
                  "recipient": "John Doe",
                  "amount": 100.50
                }
                """;

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/send-payment-record/process")
                .then()
                .statusCode(200); // Missing non-required fields still works
    }
}
