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

package org.pipelineframework.csv.orchestrator.service;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@QuarkusIntegrationTest
class CsvPaymentsEndToEndIT {

    private static final Logger LOG = Logger.getLogger(CsvPaymentsEndToEndIT.class);

    private static final Network network = Network.newNetwork();

    private static final String TEST_E2E_DIR = "target/test-e2e";
    private static final String TEST_E2E_TARGET_DIR = "/app/test-e2e";

    // Define containers for each service
    static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("quarkus")
                    .withUsername("quarkus")
                    .withPassword("quarkus")
                    .withNetwork(network)
                    .withNetworkAliases("postgres")
                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    static GenericContainer<?> inputCsvService =
            new GenericContainer<>("localhost/csv-payments/input-csv-file-processing-svc:latest")
                    .withNetwork(network)
                    .withNetworkAliases("input-csv-file-processing-svc")
                    .withFileSystemBind(
                            Paths.get(TEST_E2E_DIR).toAbsolutePath().toString(),
                            TEST_E2E_TARGET_DIR,
                            BindMode.READ_ONLY)
                    .withExposedPorts(8444)
                    .withEnv("QUARKUS_PROFILE", "test")
                    .withEnv(
                            "QUARKUS_DATASOURCE_REACTIVE_URL", "postgresql://postgres:5432/quarkus")
                    .withEnv("QUARKUS_DATASOURCE_USERNAME", "quarkus")
                    .withEnv("QUARKUS_DATASOURCE_PASSWORD", "quarkus")
                    .waitingFor(
                            Wait.forHttps("/q/health")
                                    .forPort(8444)
                                    .allowInsecure()
                                    .withStartupTimeout(Duration.ofSeconds(60)));

    static GenericContainer<?> paymentsProcessingService =
            new GenericContainer<>("localhost/csv-payments/payments-processing-svc:latest")
                    .withNetwork(network)
                    .withNetworkAliases("payments-processing-svc")
                    .withExposedPorts(8445)
                    .withEnv("QUARKUS_PROFILE", "test")
                    .withEnv(
                            "QUARKUS_DATASOURCE_REACTIVE_URL", "postgresql://postgres:5432/quarkus")
                    .withEnv("QUARKUS_DATASOURCE_USERNAME", "quarkus")
                    .withEnv("QUARKUS_DATASOURCE_PASSWORD", "quarkus")
                    .waitingFor(
                            Wait.forHttps("/q/health")
                                    .forPort(8445)
                                    .allowInsecure()
                                    .withStartupTimeout(Duration.ofSeconds(60)));

    static GenericContainer<?> paymentStatusService =
            new GenericContainer<>("localhost/csv-payments/payment-status-svc:latest")
                    .withNetwork(network)
                    .withNetworkAliases("payment-status-svc")
                    .withExposedPorts(8446)
                    .withEnv("QUARKUS_PROFILE", "test")
                    .withEnv(
                            "QUARKUS_DATASOURCE_REACTIVE_URL", "postgresql://postgres:5432/quarkus")
                    .withEnv("QUARKUS_DATASOURCE_USERNAME", "quarkus")
                    .withEnv("QUARKUS_DATASOURCE_PASSWORD", "quarkus")
                    .waitingFor(
                            Wait.forHttps("/q/health")
                                    .forPort(8446)
                                    .allowInsecure()
                                    .withStartupTimeout(Duration.ofSeconds(60)));

    static GenericContainer<?> outputCsvService =
            new GenericContainer<>("localhost/csv-payments/output-csv-file-processing-svc:latest")
                    .withNetwork(network)
                    .withNetworkAliases("output-csv-file-processing-svc")
                    .withFileSystemBind(
                            Paths.get(TEST_E2E_DIR).toAbsolutePath().toString(),
                            TEST_E2E_TARGET_DIR,
                            BindMode.READ_WRITE)
                    .withExposedPorts(8447)
                    .withEnv("QUARKUS_PROFILE", "test")
                    .withEnv(
                            "QUARKUS_DATASOURCE_REACTIVE_URL", "postgresql://postgres:5432/quarkus")
                    .withEnv("QUARKUS_DATASOURCE_USERNAME", "quarkus")
                    .withEnv("QUARKUS_DATASOURCE_PASSWORD", "quarkus")
                    .waitingFor(
                            Wait.forHttps("/q/health")
                                    .forPort(8447)
                                    .allowInsecure()
                                    .withStartupTimeout(Duration.ofSeconds(60)));

    @BeforeAll
    static void startServices() {
        // Start database first
        postgresContainer.start();

        // Start all services
        inputCsvService.start();
        paymentsProcessingService.start();
        paymentStatusService.start();
        outputCsvService.start();
    }

    @Test
    void fullPipelineWorks() throws Exception {
        LOG.info("Running full end-to-end pipeline test");

        // Create test input directory
        Path dir = Paths.get(TEST_E2E_DIR);
        Files.createDirectories(dir);

        // Clean up any existing test files
        cleanTestOutputDirectory(dir);

        // Create test CSV files as the shell script does
        createTestCsvFiles();

        // Trigger the orchestrator to process the input directory
        orchestratorTriggerRun(TEST_E2E_TARGET_DIR);

        // Wait for the pipeline to complete
        waitForPipelineComplete();

        // Verify the output files are generated with expected content
        verifyOutputFiles(TEST_E2E_DIR);

        // Verify database persistence
        verifyDatabasePersistence();

        LOG.info("End-to-end processing test completed successfully!");
    }

    @SuppressWarnings("SameParameterValue")
    private void orchestratorTriggerRun(String inputDir) throws Exception {
        LOG.infof("Triggering Orchestrator with input dir: %s", inputDir);

        ProcessBuilder pb =
                new ProcessBuilder(
                        "java",
                        "--enable-preview",
                        "-jar",
                        "target/quarkus-app/quarkus-run.jar",
                        "-i=" + inputDir);

        pb.environment().put("QUARKUS_PROFILE", "test");
        pb.environment().put("QUARKUS_JIB_JVM_ADDITIONAL_ARGUMENTS", "--enable-preview");

        pb.environment().put("QUARKUS_GRPC_CLIENTS_PROCESS_FOLDER_HOST", inputCsvService.getHost());
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_FOLDER_PORT",
                        String.valueOf(inputCsvService.getMappedPort(8444)));
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_CSV_PAYMENTS_INPUT_FILE_HOST",
                        inputCsvService.getHost());
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_CSV_PAYMENTS_INPUT_FILE_PORT",
                        String.valueOf(inputCsvService.getMappedPort(8444)));
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_SEND_PAYMENT_RECORD_HOST",
                        paymentsProcessingService.getHost());
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_SEND_PAYMENT_RECORD_PORT",
                        String.valueOf(paymentsProcessingService.getMappedPort(8445)));
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_ACK_PAYMENT_SENT_HOST",
                        paymentsProcessingService.getHost());
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_ACK_PAYMENT_SENT_PORT",
                        String.valueOf(paymentsProcessingService.getMappedPort(8445)));
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_PAYMENT_STATUS_HOST",
                        paymentStatusService.getHost());
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_PAYMENT_STATUS_PORT",
                        String.valueOf(paymentStatusService.getMappedPort(8446)));
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_CSV_PAYMENTS_OUTPUT_FILE_HOST",
                        outputCsvService.getHost());
        pb.environment()
                .put(
                        "QUARKUS_GRPC_CLIENTS_PROCESS_CSV_PAYMENTS_OUTPUT_FILE_PORT",
                        String.valueOf(outputCsvService.getMappedPort(8447)));

        pb.inheritIO();
        Process p = pb.start();
        int exitCode = p.waitFor();
        assertEquals(0, exitCode, "Orchestrator exited with non-zero code");
    }

    private void cleanTestOutputDirectory(Path outputDir) throws IOException {
        // Delete any existing CSV and OUT files in the test output directory
        if (Files.exists(outputDir)) {
            try (var files = Files.list(outputDir)) {
                files.filter(
                                path ->
                                        path.toString().endsWith(".csv")
                                                || path.toString().endsWith(".out"))
                        .forEach(
                                path -> {
                                    try {
                                        Files.deleteIfExists(path);
                                        LOG.infof("Deleted existing file: %s", path);
                                    } catch (IOException e) {
                                        LOG.warnf(e, "Failed to delete existing file: %s", path);
                                    }
                                });
            }
        }
    }

    private void createTestCsvFiles() throws IOException {
        LOG.info("Creating test CSV files...");

        // Create first test file with 3 records
        Path firstFile = Paths.get(TEST_E2E_DIR, "payments_first.csv");
        Files.write(
                firstFile,
                """
            ID,Recipient,Amount,Currency
            1,John Doe,100.00,USD
            2,Jane Smith,200.00,EUR
            3,Bob Johnson,300.00,GBP
            """
                        .getBytes());

        // Create second test file with 2 records
        Path secondFile = Paths.get(TEST_E2E_DIR, "payments_second.csv");
        Files.write(
                secondFile,
                """
            ID,Recipient,Amount,Currency
            1,Alice Brown,150.00,AUD
            2,Charlie Wilson,250.00,CAD
            """
                        .getBytes());

        LOG.info("Created test CSV files:");
        try (var files = Files.list(Paths.get(TEST_E2E_DIR))) {
            files.filter(path -> path.toString().endsWith(".csv"))
                    .forEach(path -> LOG.infof("- %s", path));
        }
    }

    @SuppressWarnings("BusyWait")
    private void waitForPipelineComplete() throws InterruptedException, IOException {
        LOG.info("Waiting for pipeline to complete processing...");

        // Check for output files to be created before continuing
        long startTime = System.currentTimeMillis();
        long timeout = TimeUnit.SECONDS.toMillis(10); // 10-second timeout

        while (System.currentTimeMillis() - startTime < timeout) {
            // Check if output files exist in the expected output directory
            boolean outputFilesExist;
            try (var files = Files.list(Paths.get(TEST_E2E_DIR))) {
                outputFilesExist = files.anyMatch(path -> path.toString().endsWith(".out"));
            }

            if (outputFilesExist) {
                LOG.info("Output files detected, pipeline processing completed");
                return;
            }

            Thread.sleep(1000); // Check every second
        }

        LOG.warn("Pipeline completion timeout reached, proceeding with verification anyway");
    }

    @SuppressWarnings("SameParameterValue")
    private void verifyOutputFiles(String testOutputTargetDir) throws IOException {
        LOG.info("Verifying output files...");

        // Check if output files exist in the output directory
        List<Path> outputFiles;
        try (var files = Files.list(Paths.get(testOutputTargetDir))) {
            outputFiles = files.filter(path -> path.toString().endsWith(".out")).toList();
        }

        // Output files should be generated
        assertFalse(outputFiles.isEmpty(), "Output files should be generated");

        LOG.info("Found output files: " + outputFiles);

        // Count total records in all output files (excluding headers)
        long totalRecords =
                outputFiles.stream()
                        .mapToLong(
                                path -> {
                                    try {
                                        List<String> lines = Files.readAllLines(path);
                                        // Exclude header line (first line)
                                        return Math.max(0, lines.size() - 1);
                                    } catch (IOException e) {
                                        LOG.warnf(e, "Failed to read file: %s", path);
                                        return 0L;
                                    }
                                })
                        .sum();

        LOG.infof("Total records across all output files: %d", totalRecords);

        // Expected: at least 5 records total (3 from first file + 2 from second file)
        assertTrue(
                totalRecords >= 5,
                String.format("Expected at least 5 records, but found %d", totalRecords));

        // Verify content patterns exist in output files
        boolean johnDoeFound = false;
        boolean janeSmithFound = false;
        boolean bobJohnsonFound = false;
        boolean aliceBrownFound = false;
        boolean charlieWilsonFound = false;

        for (Path outputFile : outputFiles) {
            String content = Files.readString(outputFile);
            if (content.contains("John Doe")) johnDoeFound = true;
            if (content.contains("Jane Smith")) janeSmithFound = true;
            if (content.contains("Bob Johnson")) bobJohnsonFound = true;
            if (content.contains("Alice Brown")) aliceBrownFound = true;
            if (content.contains("Charlie Wilson")) charlieWilsonFound = true;
        }

        assertTrue(johnDoeFound, "John Doe record should be found in output");
        assertTrue(janeSmithFound, "Jane Smith record should be found in output");
        assertTrue(bobJohnsonFound, "Bob Johnson record should be found in output");
        assertTrue(aliceBrownFound, "Alice Brown record should be found in output");
        assertTrue(charlieWilsonFound, "Charlie Wilson record should be found in output");

        LOG.info("All expected records found in output files");
    }

    private void verifyDatabasePersistence() throws Exception {
        LOG.info("Verifying database persistence...");

        // Connect to the database using the test container's connection details
        String jdbcUrl = postgresContainer.getJdbcUrl();
        String username = postgresContainer.getUsername();
        String password = postgresContainer.getPassword();

        // Load the PostgreSQL JDBC driver
        Class.forName("org.postgresql.Driver");

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            // Verify that the paymentrecord table exists
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables =
                    metaData.getTables(null, null, "paymentrecord", new String[] {"TABLE"});

            assertTrue(tables.next(), "paymentrecord table should exist in the database");

            // Query the paymentrecord table to ensure records were persisted
            String query = "SELECT COUNT(*) FROM paymentrecord";
            try (PreparedStatement stmt = connection.prepareStatement(query);
                    ResultSet rs = stmt.executeQuery()) {

                assertTrue(rs.next(), "Query should return results");
                int recordCount = rs.getInt(1);

                LOG.infof("Found %d records in paymentrecord table", recordCount);

                // Expected: 5 records total (3 from first file + 2 from second file)
                assertEquals(
                        5,
                        recordCount,
                        String.format("Expected 5 records in database, but found %d", recordCount));
            }

            // Verify specific records exist in the database
            verifySpecificRecordsInDatabase(connection);

            LOG.info("Database verification completed successfully");
        }
    }

    private void verifySpecificRecordsInDatabase(Connection connection) throws SQLException {
        LOG.info("Verifying specific records in database...");

        // Check for records from first file
        String[] expectedRecords = {
            "John Doe", "Jane Smith", "Bob Johnson", "Alice Brown", "Charlie Wilson"
        };

        for (String recordName : expectedRecords) {
            String query = "SELECT COUNT(*) FROM paymentrecord WHERE recipient = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, recordName);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "Query should return results for " + recordName);
                    int count = rs.getInt(1);
                    assertTrue(
                            count > 0,
                            "Record for " + recordName + " should exist in the database");
                }
            }
        }

        LOG.info("All expected records found in database");
    }

    @AfterAll
    static void tearDown() {
        // Stop all containers to prevent resource leaks
        if (outputCsvService != null && outputCsvService.isRunning()) {
            outputCsvService.stop();
        }
        if (paymentStatusService != null && paymentStatusService.isRunning()) {
            paymentStatusService.stop();
        }
        if (paymentsProcessingService != null && paymentsProcessingService.isRunning()) {
            paymentsProcessingService.stop();
        }
        if (inputCsvService != null && inputCsvService.isRunning()) {
            inputCsvService.stop();
        }
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
        }
        if (network != null) {
            network.close();
        }
    }
}
