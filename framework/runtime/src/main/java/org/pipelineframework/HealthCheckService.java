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

package org.pipelineframework;

import jakarta.enterprise.context.ApplicationScoped;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

@ApplicationScoped
public class HealthCheckService {

    private static final Logger LOG = Logger.getLogger(HealthCheckService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .sslContext(createInsecureSslContext())
            .build();


    /**
     * Creates an SSL context that ignores certificate validation, similar to curl -k
     */
    private javax.net.ssl.SSLContext createInsecureSslContext() {
        try {
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    }
            };
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext;
        } catch (Exception e) {
            LOG.warn("Failed to create insecure SSL context, proceeding with default", e);
            try {
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getDefault();
                return sslContext;
            } catch (Exception ex) {
                LOG.error("Failed to get default SSL context", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Checks the health of all dependent services before running the pipeline.
     * This method inspects each step to detect gRPC client dependencies and checks their health endpoints.
     * Retries every 5 seconds for up to 2 minutes before giving up.
     *
     * @param steps the list of pipeline steps to check for dependent services
     * @return true if all dependent services are healthy, false otherwise
     */
    public boolean checkHealthOfDependentServices(List<Object> steps) {
        LOG.info("Checking health of dependent services before pipeline execution...");

        // Extract all gRPC client names from the steps
        Set<String> grpcClientNames = new HashSet<>();
        for (Object step : steps) {
            if (step != null) {
                Set<String> stepClientNames = extractGrpcClientNames(step);
                grpcClientNames.addAll(stepClientNames);
            }
        }

        if (grpcClientNames.isEmpty()) {
            LOG.info("No gRPC client dependencies found. Proceeding with pipeline execution.");
            return true;
        }

        int maxRetries = 24; // 2 minutes / 5 seconds = 24 checks
        int retryIntervalSeconds = 5;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            boolean allHealthy = true;
            Set<String> unhealthyServices = new HashSet<>();

            // Check the health of each gRPC client endpoint
            for (String grpcClientName : grpcClientNames) {
                if (!isGrpcClientServiceHealthy(grpcClientName)) {
                    allHealthy = false;
                    unhealthyServices.add(grpcClientName);
                }
            }

            if (allHealthy) {
                LOG.info("All dependent services are healthy. Proceeding with pipeline execution.");
                return true;
            } else {
                LOG.warn("Attempt " + (attempt + 1) + " failed. Services not healthy: " + unhealthyServices);
                if (attempt < maxRetries - 1) { // Don't sleep on the last attempt
                    LOG.info("Retrying health checks in " + retryIntervalSeconds + " seconds...");
                    try {
                        Thread.sleep(retryIntervalSeconds * 1000L);
                    } catch (InterruptedException e) {
                        LOG.warn("Health check interrupted, aborting pipeline execution...");
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        LOG.error("Health checks failed after " + maxRetries + " attempts. Pipeline execution will not proceed.");
        return false;
    }

    /**
     * Checks the health of a gRPC client service by attempting to access its health endpoint.
     * Uses the gRPC client configuration from the application properties to determine the host and port.
     *
     * @param grpcClientName the name of the gRPC client
     * @return true if the service is healthy, false otherwise
     */
    private boolean isGrpcClientServiceHealthy(String grpcClientName) {
        try {
            // Get host and port from configuration
            String host = ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.grpc.clients." + grpcClientName + ".host", String.class)
                    .orElse("localhost");

            String portStr = ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.grpc.clients." + grpcClientName + ".port", String.class)
                    .orElse("8080");

            int port = Integer.parseInt(portStr);

            // Determine if the service uses HTTPS based on TLS configuration
            boolean useTls = ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.grpc.clients." + grpcClientName + ".tls.enabled", Boolean.class)
                    .orElse(false);

            // Construct the health check URL (typically served at /q/health)
            String protocol = useTls ? "https" : "http";
            String healthUrl = String.format("%s://%s:%d/q/health", protocol, host, port);

            // Create and execute the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(5))  // Increase timeout
                    .GET()
                    .build();

            // Use sendAsync with a timeout to avoid blocking indefinitely and handle interruptions better
            CompletableFuture<HttpResponse<String>> responseFuture =
                    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            // Wait for the response with timeout
            HttpResponse<String> response = responseFuture
                    .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .join(); // This is safe in this context since we have timeout handling

            boolean isHealthy = response.statusCode() == 200;
            if (isHealthy) {
                LOG.info("✓ gRPC client '" + grpcClientName + "' service at " + host + ":" + port + " is healthy");
            } else {
                LOG.info("✗ gRPC client '" + grpcClientName + "' service at " + host + ":" + port + " is not healthy. Status: " + response.statusCode());
            }

            return isHealthy;
        } catch (java.util.concurrent.CompletionException e) {
            // Handle TimeoutException and other completion-related exceptions
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                LOG.info("✗ gRPC client '" + grpcClientName + "' service health check timed out");
            } else {
                LOG.info("✗ gRPC client '" + grpcClientName + "' service is not accessible. Error: " + e.getCause().getMessage());
            }
            return false;
        } catch (Exception e) {
            LOG.info("✗ Error checking health of gRPC client '" + grpcClientName + "' service. Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts gRPC client names from the fields of a step object using reflection.
     *
     * @param step the step object to inspect
     * @return a set of gRPC client names used by the step
     */
    private Set<String> extractGrpcClientNames(Object step) {
        Set<String> grpcClientNames = new HashSet<>();
        Class<?> stepClass = step.getClass();

        // Check all fields in the step class
        for (Field field : stepClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(io.quarkus.grpc.GrpcClient.class)) {
                io.quarkus.grpc.GrpcClient grpcClientAnnotation = field.getAnnotation(io.quarkus.grpc.GrpcClient.class);
                String clientName = grpcClientAnnotation.value();

                // If the value is empty, use the field name as default
                if (clientName.isEmpty()) {
                    clientName = field.getName();
                }

                grpcClientNames.add(clientName);
            }
        }

        return grpcClientNames;
    }
}