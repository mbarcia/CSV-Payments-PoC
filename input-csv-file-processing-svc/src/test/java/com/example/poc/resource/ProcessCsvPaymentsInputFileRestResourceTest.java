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
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProcessCsvPaymentsInputFileRestResourceTest {

  @Test
  void testProcessEndpointWithNonExistentFile() {
    // Create a test DTO with a non-existent file
    String requestBody =
        """
        {
          "id": "%s",
          "filepath": "%s",
          "csvFolderPath": "%s"
        }
        """
            .formatted(UUID.randomUUID(), "/tmp/nonexistent.csv", "/tmp");

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/api/v1/csv-processing/process")
        .then()
        .statusCode(400)
        .body(containsString("Error processing file"));
  }

  @Test
  void testProcessToListEndpointWithNonExistentFile() {
    // Create a test DTO with a non-existent file
    String requestBody =
        """
        {
          "id": "%s",
          "filepath": "%s",
          "csvFolderPath": "%s"
        }
        """
            .formatted(UUID.randomUUID(), "/tmp/nonexistent.csv", "/tmp");

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/api/v1/csv-processing/process-list")
        .then()
        .statusCode(400)
        .body(containsString("Error processing file"));
  }

  @Test
  void testProcessEndpointWithInvalidData() {
    // Create a test DTO with invalid data that will cause a runtime exception
    String requestBody =
        """
        {
          "id": "invalid-uuid",
          "filepath": "%s",
          "csvFolderPath": "%s"
        }
        """
            .formatted("/tmp/test.csv", "/tmp");

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/api/v1/csv-processing/process")
        .then()
        .statusCode(400);
  }

  @Test
  void testProcessToListEndpointWithInvalidData() {
    // Create a test DTO with invalid data that will cause a runtime exception
    String requestBody =
        """
        {
          "id": "invalid-uuid",
          "filepath": "%s",
          "csvFolderPath": "%s"
        }
        """
            .formatted("/tmp/test.csv", "/tmp");

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/api/v1/csv-processing/process-list")
        .then()
        .statusCode(400);
  }

  @Test
  void testProcessEndpointWithValidStructure() {
    // Create a test DTO with valid structure but non-existent file
    String requestBody =
        """
        {
          "id": "%s",
          "filepath": "%s",
          "csvFolderPath": "%s"
        }
        """
            .formatted(UUID.randomUUID(), "/tmp/test.csv", "/tmp");

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/api/v1/csv-processing/process")
        .then()
        .statusCode(400);
  }

  @Test
  void testProcessToListEndpointWithValidStructure() {
    // Create a test DTO with valid structure but non-existent file
    String requestBody =
        """
        {
          "id": "%s",
          "filepath": "%s",
          "csvFolderPath": "%s"
        }
        """
            .formatted(UUID.randomUUID(), "/tmp/test.csv", "/tmp");

    given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/api/v1/csv-processing/process-list")
        .then()
        .statusCode(400);
  }
}
