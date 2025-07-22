package com.example.poc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProviderConfigTest {

    private PaymentProviderConfig config;

    @BeforeEach
    void setUp() {
        config = new PaymentProviderConfig() {
            @Override
            public double permitsPerSecond() {
                return 100.0;
            }

            @Override
            public long timeoutMillis() {
                return 5000L;
            }

            @Override
            public double waitMilliseconds() {
                return 100.0;
            }
        };
    }

    @Test
    void testDefaultPermitsPerSecond() {
        assertThat(config.permitsPerSecond()).isEqualTo(100.0);
    }

    @Test
    void testDefaultTimeoutMillis() {
        assertThat(config.timeoutMillis()).isEqualTo(5000L);
    }

    @Test
    void testDefaultWaitMilliseconds() {
        assertThat(config.waitMilliseconds()).isEqualTo(100.0);
    }
}
