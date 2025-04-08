package com.example.poc.service;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class PaymentProviderConfig {
    private static final String PERMITS_KEY = "payment.provider.permits-per-second";
    private static final String TIMEOUT_KEY = "payment.provider.timeout-millis";

    private final double permitsPerSecond;
    private final long timeoutMillis;

    public PaymentProviderConfig() {
        // Load from properties file using standard Java APIs
        Properties props = loadProperties();
        this.permitsPerSecond = Double.parseDouble(
                props.getProperty(PERMITS_KEY, "10.0"));
        this.timeoutMillis = Long.parseLong(
                props.getProperty(TIMEOUT_KEY, "5000"));
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println("Failed to load application.properties: " + e.getMessage());
        }
        return props;
    }
}
