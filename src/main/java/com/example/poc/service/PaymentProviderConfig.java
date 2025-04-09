package com.example.poc.service;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "csv-poc.payment-provider")
public interface PaymentProviderConfig {
    @WithDefault("10.0")
    double permitsPerSecond();

    @WithDefault("5000")
    long timeoutMillis();
}
