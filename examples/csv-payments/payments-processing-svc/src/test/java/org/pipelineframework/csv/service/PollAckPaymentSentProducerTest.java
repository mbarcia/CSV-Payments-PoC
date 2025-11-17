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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PollAckPaymentSentProducerTest {

    @Mock
    private PollAckPaymentSentOnVirtualThreadReactiveService vthreadImpl;

    @Mock
    private PollAckPaymentSentReactiveService reactiveImpl;

    private PollAckPaymentSentProducer producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        producer = new PollAckPaymentSentProducer();
        
        // Manually inject the mocked implementations
        try {
            java.lang.reflect.Field vthreadField = 
                PollAckPaymentSentProducer.class.getDeclaredField("vthreadImpl");
            vthreadField.setAccessible(true);
            vthreadField.set(producer, vthreadImpl);

            java.lang.reflect.Field reactiveField = 
                PollAckPaymentSentProducer.class.getDeclaredField("reactiveImpl");
            reactiveField.setAccessible(true);
            reactiveField.set(producer, reactiveImpl);
        } catch (Exception e) {
            fail("Failed to inject mocks: " + e.getMessage());
        }
    }

    @Test
    void testProduceReturnsReactiveImplForPlatformThread() {
        // Given - Running on a platform thread (standard JUnit execution)
        boolean isVirtual = Thread.currentThread().isVirtual();

        // When
        PollAckPaymentSentService<?> result = producer.produce();

        // Then
        if (!isVirtual) {
            assertSame(reactiveImpl, result, 
                "Should return reactive implementation for platform thread");
        } else {
            assertSame(vthreadImpl, result, 
                "Should return vthread implementation for virtual thread");
        }
    }

    @Test
    void testProduceIsConsistent() {
        // Given/When
        PollAckPaymentSentService<?> first = producer.produce();
        PollAckPaymentSentService<?> second = producer.produce();

        // Then
        assertSame(first, second, 
            "Should return same implementation for same thread type");
    }

    @Test
    void testProduceReturnsNonNull() {
        // Given/When
        PollAckPaymentSentService<?> result = producer.produce();

        // Then
        assertNotNull(result, "Produced service should never be null");
    }

    @Test
    void testProducerHasBothImplementations() {
        // Given/When/Then
        assertNotNull(vthreadImpl, "VThread implementation should be injected");
        assertNotNull(reactiveImpl, "Reactive implementation should be injected");
    }

    @Test
    void testProduceReturnsCorrectTypeForPlatformThread() {
        // Given
        boolean isVirtual = Thread.currentThread().isVirtual();

        // When
        PollAckPaymentSentService<?> result = producer.produce();

        // Then
        if (!isVirtual) {
            assertTrue(result == reactiveImpl, 
                "Should be the reactive implementation");
        }
    }

    @Test
    void testProducerLogicBasedOnThreadType() {
        // Given
        boolean currentThreadIsVirtual = Thread.currentThread().isVirtual();

        // When
        PollAckPaymentSentService<?> result = producer.produce();

        // Then - Verify the logic matches expectations
        if (currentThreadIsVirtual) {
            assertSame(vthreadImpl, result, 
                "Virtual thread should get vthread implementation");
        } else {
            assertSame(reactiveImpl, result, 
                "Platform thread should get reactive implementation");
        }
    }
}