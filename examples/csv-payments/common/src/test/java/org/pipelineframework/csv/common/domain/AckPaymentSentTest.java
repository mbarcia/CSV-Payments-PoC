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

package org.pipelineframework.csv.common.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AckPaymentSentTest {

    @Test
    void testConstructorWithConversationId() {
        // Given
        UUID conversationId = UUID.randomUUID();

        // When
        AckPaymentSent ack = new AckPaymentSent(conversationId);

        // Then
        assertNotNull(ack.getId());
        assertInstanceOf(UUID.class, ack.getId());
        assertEquals(conversationId, ack.getConversationId());
    }

    @Test
    void testDefaultConstructor() {
        // When
        AckPaymentSent ack = new AckPaymentSent();

        // Then
        assertNotNull(ack.getId());
        assertInstanceOf(UUID.class, ack.getId());
    }

    @Test
    void testGetSetConversationId() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();
        UUID conversationId = UUID.randomUUID();

        // When
        ack.setConversationId(conversationId);

        // Then
        assertEquals(conversationId, ack.getConversationId());
    }

    @Test
    void testGetSetStatus() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();
        Long status = 200L;

        // When
        ack.setStatus(status);

        // Then
        assertEquals(status, ack.getStatus());
    }

    @Test
    void testGetSetMessage() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();
        String message = "Test message";

        // When
        ack.setMessage(message);

        // Then
        assertEquals(message, ack.getMessage());
    }

    @Test
    void testGetSetPaymentRecord() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setCsvId("test-record");

        // When
        ack.setPaymentRecord(paymentRecord);

        // Then
        assertEquals(paymentRecord, ack.getPaymentRecord());
    }

    @Test
    void testGetSetPaymentRecordId() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();
        UUID paymentRecordId = UUID.randomUUID();

        // When
        ack.setPaymentRecordId(paymentRecordId);

        // Then
        assertEquals(paymentRecordId, ack.getPaymentRecordId());
    }

    @Test
    void testToString() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();
        UUID conversationId = UUID.randomUUID();
        ack.setConversationId(conversationId);
        ack.setStatus(200L);
        ack.setMessage("Success");
        UUID paymentRecordId = UUID.randomUUID();
        ack.setPaymentRecordId(paymentRecordId);

        // When
        String result = ack.toString();

        // Then
        assertTrue(result.contains(conversationId.toString()));
        assertTrue(result.contains("200"));
        assertTrue(result.contains("Success"));
        assertTrue(result.contains(paymentRecordId.toString()));
    }

    @Test
    void testEquals_SameObject() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();

        // When
        boolean result = ack.equals(ack);

        // Then
        assertTrue(result);
    }

    @Test
    void testEquals_NullObject() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();

        // When
        boolean result = ack.equals(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testEquals_DifferentClass() {
        // Given
        AckPaymentSent ack = new AckPaymentSent();
        String other = "other";

        // When
        boolean result = ack.equals(other);

        // Then
        assertFalse(result);
    }

    @Test
    void testEquals_SameId() {
        // Given
        AckPaymentSent ack1 = new AckPaymentSent();
        AckPaymentSent ack2 = new AckPaymentSent();
        UUID id = UUID.randomUUID();
        ack1.setId(id);
        ack2.setId(id);

        // When
        boolean result = ack1.equals(ack2);

        // Then
        assertTrue(result);
    }

    @Test
    void testEquals_DifferentId() {
        // Given
        AckPaymentSent ack1 = new AckPaymentSent();
        AckPaymentSent ack2 = new AckPaymentSent();

        // When
        boolean result = ack1.equals(ack2);

        // Then
        assertFalse(result);
    }
}
