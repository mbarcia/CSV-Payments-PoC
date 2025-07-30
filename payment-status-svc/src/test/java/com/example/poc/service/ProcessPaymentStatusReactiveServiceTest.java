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

package com.example.poc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentOutput;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.domain.PaymentStatus;
import com.example.poc.common.dto.PaymentOutputDto;
import com.example.poc.common.mapper.PaymentOutputMapper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessPaymentStatusReactiveServiceTest {

  @Mock PaymentOutputMapper mapper;

  @InjectMocks ProcessPaymentStatusReactiveService service;

  @Captor ArgumentCaptor<PaymentOutputDto> dtoCaptor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void process() {
    // Given
    PaymentRecord paymentRecord =
        new com.example.poc.common.domain.PaymentRecord(
            UUID.randomUUID().toString(),
            "recipient123",
            new java.math.BigDecimal("100.00"),
            java.util.Currency.getInstance("USD"));
    AckPaymentSent ackPaymentSent =
        new AckPaymentSent(UUID.randomUUID())
            .setPaymentRecord(paymentRecord)
            .setStatus(1L)
            .setMessage("SUCCESS");
    PaymentStatus paymentStatus = mock(PaymentStatus.class);
    when(paymentStatus.getReference()).thenReturn(UUID.randomUUID().toString());
    when(paymentStatus.getAckPaymentSent()).thenReturn(ackPaymentSent);
    when(paymentStatus.getMessage()).thenReturn("Payment processed successfully");
    when(paymentStatus.getFee()).thenReturn(new java.math.BigDecimal("1.50"));
    when(paymentStatus.save()).thenReturn(Uni.createFrom().voidItem());

    PaymentOutput expectedOutput = new PaymentOutput();
    when(mapper.fromDto(dtoCaptor.capture())).thenReturn(expectedOutput);

    // When
    Uni<PaymentOutput> resultUni = service.process(paymentStatus);

    // Then
    UniAssertSubscriber<PaymentOutput> subscriber =
        resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber.awaitItem();
    PaymentOutput result = subscriber.getItem();
    assertNotNull(result);

    PaymentOutputDto capturedDto = dtoCaptor.getValue();
    assertEquals(paymentStatus, capturedDto.getPaymentStatus());
    assertEquals(paymentRecord.getCsvId(), capturedDto.getCsvId());
    assertEquals(paymentRecord.getRecipient(), capturedDto.getRecipient());
    assertEquals(paymentRecord.getAmount(), capturedDto.getAmount());
    assertEquals(paymentRecord.getCurrency(), capturedDto.getCurrency());
    assertEquals(ackPaymentSent.getConversationId().toString(), capturedDto.getConversationId());
    assertEquals(ackPaymentSent.getStatus(), capturedDto.getStatus());
    assertEquals(paymentStatus.getMessage(), capturedDto.getMessage());
    assertEquals(paymentStatus.getFee(), capturedDto.getFee());
  }
}
