package com.example.poc.service;

import com.example.poc.client.SendPaymentRequest;
import com.example.poc.domain.AckPaymentSent;
import com.example.poc.domain.PaymentRecord;
import com.example.poc.domain.PaymentStatus;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentProviderMockTest {

    private PaymentProviderMock paymentProviderMock;

    @Mock
    private PaymentRecord mockRecord;

    @Mock
    private SendPaymentRequest mockRequest;

    @Mock
    private AckPaymentSent mockAck;

    @Mock
    private PaymentProviderConfig mockConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        paymentProviderMock = new PaymentProviderMock();
        when(mockRequest.getUrl()).thenReturn("https://payment.example.com");
        when(mockRequest.getAmount()).thenReturn(new BigDecimal("123.45"));
        when(mockRequest.getMsisdn()).thenReturn("1234567890");
        when(mockRequest.getRecord()).thenReturn(mockRecord);
        when(mockRequest.getCurrency()).thenReturn(Currency.getInstance("USD"));
        when(mockRequest.getReference()).thenReturn("REF123");
        when(mockAck.getRecord()).thenReturn(mockRecord);
    }

    @Test
    void sendPayment_ShouldReturnValidAckPaymentSent() {
        // Act
        AckPaymentSent result = paymentProviderMock.sendPayment(mockRequest);

        // Assert
        assertNotNull(result);
        assertEquals(PaymentProviderMock.UUID, result.getConversationID());
        assertEquals(1000L, result.getStatus());
        assertEquals("OK but this is only a test", result.getMessage());
        assertEquals(mockRecord, result.getRecord());
    }

    @Test
    void getPaymentStatus_ShouldReturnValidPaymentStatus() {
        // Act
        PaymentStatus result = paymentProviderMock.getPaymentStatus(mockAck);

        // Assert
        assertNotNull(result);
        assertEquals("101", result.getReference());
        assertEquals("nada", result.getStatus());
        assertEquals(new BigDecimal("1.01"), result.getFee());
        assertEquals("This is a test", result.getMessage());
        assertEquals(mockAck, result.getAckPaymentSent());
    }

    @Test
    @DisplayName("Test throttling when requests exceed rate limit")
    void testThrottling() throws InterruptedException {
        // Arrange
        double rateLimit = 5.0; // 5 requests per second
        long timeoutMillis = 100; // Short timeout for testing

        PaymentProviderMock provider = new PaymentProviderMock(rateLimit, timeoutMillis);

        int numThreads = 20; // Try to make 20 requests at once
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger throttledCount = new AtomicInteger(0);

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numThreads);

        // Create a fixed thread pool
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Act - Submit tasks to the executor
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await(); // Wait for all threads to be ready
                    try {
                        provider.sendPayment(mockRequest);
                        successCount.incrementAndGet();
                    } catch (PaymentProviderMock.ThrottlingException e) {
                        throttledCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown(); // Start all threads
        doneSignal.await(2, TimeUnit.SECONDS); // Wait for all threads to complete
        executor.shutdown();

        // Assert
        assertTrue(successCount.get() > 0, "Some requests should succeed");
        assertTrue(throttledCount.get() > 0, "Some requests should be throttled");
        assertEquals(numThreads, successCount.get() + throttledCount.get(),
                "All requests should either succeed or be throttled");

        // The rate limit is 5 per second, but due to how RateLimiter works with bursts,
        // we can't predict exactly how many will succeed, just that some will be throttled
    }

    @Test
    @DisplayName("Test throttling with timeout")
    void testThrottlingWithTimeout() {
        // Arrange - Create a mock RateLimiter that always returns false for tryAcquire
        RateLimiter mockRateLimiter = mock(RateLimiter.class);
        when(mockRateLimiter.tryAcquire(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Use Mockito's static mocking to intercept the RateLimiter.create call
        try (MockedStatic<RateLimiter> mockedStatic = Mockito.mockStatic(RateLimiter.class)) {
            mockedStatic.when(() -> RateLimiter.create(anyDouble())).thenReturn(mockRateLimiter);

            // Create provider with mocked RateLimiter
            PaymentProviderMock provider = new PaymentProviderMock(10.0, 50);

            // Act & Assert for sendPayment
            PaymentProviderMock.ThrottlingException exception = assertThrows(
                    PaymentProviderMock.ThrottlingException.class,
                    () -> provider.sendPayment(mockRequest)
            );
            assertTrue(exception.getMessage().contains("Failed to acquire permit within timeout period"));

            // Act & Assert for getPaymentStatus
            exception = assertThrows(
                    PaymentProviderMock.ThrottlingException.class,
                    () -> provider.getPaymentStatus(mockAck)
            );
            assertTrue(exception.getMessage().contains("Failed to acquire permit within timeout period"));
        }
    }

    @Test
    @DisplayName("Test default constructor uses configuration correctly")
    void testDefaultConstructor() {
        // Create a mock PaymentProviderConfig to inject
        when(mockConfig.getPermitsPerSecond()).thenReturn(15.0);
        when(mockConfig.getTimeoutMillis()).thenReturn(2500L);

        PaymentProviderMock provider = new PaymentProviderMock(mockConfig);

        // Verify the provider was created with the right config - we'll test indirectly
        // by ensuring calls succeed (i.e., the rateLimiter allows them)
        assertDoesNotThrow(() -> provider.sendPayment(mockRequest));
        assertDoesNotThrow(() -> provider.getPaymentStatus(mockAck));
    }

    @Test
    @DisplayName("Test concurrent requests with virtual threads")
    void testConcurrentRequestsWithVirtualThreads() throws InterruptedException {
        // This test requires Java 21 for virtual threads
        // If using earlier versions, modify to use Thread.startVirtualThread or remove this test

        // Arrange
        PaymentProviderMock provider = new PaymentProviderMock(10.0, 1000);
        int requestCount = 30;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger throttledCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(requestCount);

        // Act - Create and start virtual threads
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            Thread t = Thread.ofVirtual().start(() -> {
                try {
                    provider.sendPayment(mockRequest);
                    successCount.incrementAndGet();
                } catch (PaymentProviderMock.ThrottlingException e) {
                    throttledCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
            threads.add(t);
        }

        // Wait for all threads to complete
        latch.await(3, TimeUnit.SECONDS);

        // Assert
        assertEquals(requestCount, successCount.get() + throttledCount.get(),
                "All requests should be accounted for");
        // Since rate limit is 10/sec and processing is quick, we expect about 10 to succeed
        // immediately and the others to either succeed later or be throttled depending on timeout
    }
}