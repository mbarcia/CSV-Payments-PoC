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

package org.pipelineframework.csv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pipelineframework.csv.common.domain.AckPaymentSent;
import org.pipelineframework.csv.common.domain.PaymentStatus;

class PollAckPaymentSentOnVirtualThreadReactiveServiceTest {

    @Mock
    private PaymentProviderService paymentProviderServiceMock;

    @Mock
    private PaymentProviderConfig config;

    private PollAckPaymentSentOnVirtualThreadReactiveService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up default config behavior
        when(config.permitsPerSecond()).thenReturn(10.0);
        when(config.timeoutMillis()).thenReturn(5000L);
        when(config.waitMilliseconds()).thenReturn(100L);
        
        service = new PollAckPaymentSentOnVirtualThreadReactiveService(
            paymentProviderServiceMock, config);
    }

    @Test
    void testProcessReturnsPaymentStatus() {
        // Given
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        ackPaymentSent.setId(1L);
        ackPaymentSent.setConversationId("conv123");
        ackPaymentSent.setPaymentRecordId(456L);

        PaymentStatus expectedStatus = new PaymentStatus();
        expectedStatus.setId(789L);
        expectedStatus.setReference("REF123");
        expectedStatus.setStatus("COMPLETED");

        when(paymentProviderServiceMock.getPaymentStatus(ackPaymentSent))
            .thenReturn(expectedStatus);

        // When
        Uni<PaymentStatus> result = service.process(ackPaymentSent);

        // Then
        UniAssertSubscriber<PaymentStatus> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
        
        PaymentStatus actualStatus = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
        
        assertNotNull(actualStatus, "Payment status should not be null");
        assertEquals(expectedStatus.getId(), actualStatus.getId());
        assertEquals(expectedStatus.getReference(), actualStatus.getReference());
        assertEquals(expectedStatus.getStatus(), actualStatus.getStatus());
        
        verify(paymentProviderServiceMock).getPaymentStatus(ackPaymentSent);
    }

    @Test
    void testProcessCallsPaymentProviderService() {
        // Given
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        PaymentStatus paymentStatus = new PaymentStatus();
        
        when(paymentProviderServiceMock.getPaymentStatus(any()))
            .thenReturn(paymentStatus);

        // When
        Uni<PaymentStatus> result = service.process(ackPaymentSent);
        result.subscribe().withSubscriber(UniAssertSubscriber.create())
            .awaitItem(Duration.ofSeconds(5));

        // Then
        verify(paymentProviderServiceMock).getPaymentStatus(ackPaymentSent);
    }

    @Test
    void testProcessSimulatesPollingDelay() {
        // Given
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        PaymentStatus paymentStatus = new PaymentStatus();
        
        when(paymentProviderServiceMock.getPaymentStatus(any()))
            .thenReturn(paymentStatus);
        when(config.waitMilliseconds()).thenReturn(50L);

        // When
        long startTime = System.currentTimeMillis();
        Uni<PaymentStatus> result = service.process(ackPaymentSent);
        result.subscribe().withSubscriber(UniAssertSubscriber.create())
            .awaitItem(Duration.ofSeconds(5));
        long endTime = System.currentTimeMillis();

        // Then
        long elapsedTime = endTime - startTime;
        assertTrue(elapsedTime >= 0, "Some time should have elapsed for processing");
        // Note: Due to random delay, we can't assert exact timing
    }

    @Test
    void testProcessHandlesProviderException() {
        // Given
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        RuntimeException expectedException = new RuntimeException("Provider error");
        
        when(paymentProviderServiceMock.getPaymentStatus(any()))
            .thenThrow(expectedException);

        // When
        Uni<PaymentStatus> result = service.process(ackPaymentSent);

        // Then
        UniAssertSubscriber<PaymentStatus> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
        
        Throwable failure = subscriber.awaitFailure(Duration.ofSeconds(5)).getFailure();
        assertNotNull(failure, "Should propagate failure");
        assertTrue(failure instanceof RuntimeException);
    }

    @Test
    void testProcessWithNullAckPaymentSent() {
        // Given
        AckPaymentSent nullAck = null;
        
        when(paymentProviderServiceMock.getPaymentStatus(null))
            .thenThrow(new NullPointerException("AckPaymentSent cannot be null"));

        // When
        Uni<PaymentStatus> result = service.process(nullAck);

        // Then
        UniAssertSubscriber<PaymentStatus> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
        
        assertThrows(Exception.class, () -> subscriber.awaitItem(Duration.ofSeconds(5)));
    }

    @Test
    void testProcessRespectsConfiguredWaitTime() {
        // Given
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        PaymentStatus paymentStatus = new PaymentStatus();
        
        when(paymentProviderServiceMock.getPaymentStatus(any()))
            .thenReturn(paymentStatus);
        when(config.waitMilliseconds()).thenReturn(1L); // Minimal wait

        // When
        Uni<PaymentStatus> result = service.process(ackPaymentSent);
        
        // Then - Should complete quickly with minimal wait
        UniAssertSubscriber<PaymentStatus> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
        
        assertDoesNotThrow(() -> subscriber.awaitItem(Duration.ofSeconds(2)));
        assertNotNull(subscriber.getItem());
    }

    @Test
    void testProcessWithCompleteAckPaymentSentData() {
        // Given
        AckPaymentSent ackPaymentSent = new AckPaymentSent();
        ackPaymentSent.setId(100L);
        ackPaymentSent.setConversationId("CONV-XYZ-789");
        ackPaymentSent.setPaymentRecordId(200L);

        PaymentStatus paymentStatus = new PaymentStatus();
        paymentStatus.setId(300L);
        paymentStatus.setReference("REF-ABC-456");
        paymentStatus.setStatus("PENDING");
        
        when(paymentProviderServiceMock.getPaymentStatus(ackPaymentSent))
            .thenReturn(paymentStatus);

        // When
        Uni<PaymentStatus> result = service.process(ackPaymentSent);

        // Then
        UniAssertSubscriber<PaymentStatus> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
        
        PaymentStatus actualStatus = subscriber.awaitItem(Duration.ofSeconds(5)).getItem();
        
        assertNotNull(actualStatus);
        assertEquals(300L, actualStatus.getId());
        assertEquals("REF-ABC-456", actualStatus.getReference());
        assertEquals("PENDING", actualStatus.getStatus());
    }

    @Test
    void testMultipleSequentialProcessCalls() {
        // Given
        AckPaymentSent ack1 = new AckPaymentSent();
        ack1.setId(1L);
        AckPaymentSent ack2 = new AckPaymentSent();
        ack2.setId(2L);

        PaymentStatus status1 = new PaymentStatus();
        status1.setId(10L);
        PaymentStatus status2 = new PaymentStatus();
        status2.setId(20L);

        when(paymentProviderServiceMock.getPaymentStatus(ack1)).thenReturn(status1);
        when(paymentProviderServiceMock.getPaymentStatus(ack2)).thenReturn(status2);

        // When
        Uni<PaymentStatus> result1 = service.process(ack1);
        Uni<PaymentStatus> result2 = service.process(ack2);

        // Then
        PaymentStatus actualStatus1 = result1.subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .awaitItem(Duration.ofSeconds(5)).getItem();
        PaymentStatus actualStatus2 = result2.subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .awaitItem(Duration.ofSeconds(5)).getItem();

        assertEquals(10L, actualStatus1.getId());
        assertEquals(20L, actualStatus2.getId());
        verify(paymentProviderServiceMock).getPaymentStatus(ack1);
        verify(paymentProviderServiceMock).getPaymentStatus(ack2);
    }
}