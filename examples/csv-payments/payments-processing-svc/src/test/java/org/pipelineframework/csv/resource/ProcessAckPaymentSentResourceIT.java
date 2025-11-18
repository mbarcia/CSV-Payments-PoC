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
class ProcessAckPaymentSentResourceIT {

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
    void testProcessAckPaymentEndpointWithValidData() {
        // Create a test DTO with valid structure
        String requestBody =
                """
                {
                  "id": "%s",
                  "conversationId": "%s",
                  "paymentRecordId": "%s",
                  "message": "Payment sent successfully",
                  "status": 200
                }
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/process-ack-payment-sent/process")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("reference", notNullValue())
                .body("status", notNullValue());
    }

    @Test
    void testProcessAckPaymentEndpointWithInvalidUUID() {
        // Create a test DTO with invalid UUID
        String requestBody =
                """
                {
                  "id": "invalid-uuid",
                  "conversationId": "invalid-uuid",
                  "paymentRecordId": "invalid-uuid",
                  "message": "Payment sent successfully",
                  "status": 200
                }
                """;

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/process-ack-payment-sent/process")
                .then()
                .statusCode(500);
    }

    @Test
    void testProcessAckPaymentEndpointWithMissingRequiredFields() {
        // Create a test DTO with missing required fields but with valid conversationId
        String requestBody =
                """
                {
                  "conversationId": "%s",
                  "message": "Payment sent successfully",
                  "status": 200
                }
                """
                        .formatted(UUID.randomUUID());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/process-ack-payment-sent/process")
                .then()
                .statusCode(
                        400); // Missing required fields in the response object results in 400 Bad
        // Request
    }
}
