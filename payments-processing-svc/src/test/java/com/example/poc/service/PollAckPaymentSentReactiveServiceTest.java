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
import static org.mockito.Mockito.when;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.smallrye.mutiny.Uni;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PollAckPaymentSentReactiveServiceTest {

  @Mock private PaymentProviderService paymentProviderService;
  @Mock private PaymentProviderConfig paymentProviderConfig;

  private PollAckPaymentSentReactiveService pollAckPaymentSentReactiveService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    pollAckPaymentSentReactiveService =
        new PollAckPaymentSentReactiveService(
            Executors.newVirtualThreadPerTaskExecutor(),
            paymentProviderService,
            paymentProviderConfig);
  }

  @Test
  void testExecute() throws JsonProcessingException {
    // Given
    AckPaymentSent ackPaymentSent = new AckPaymentSent();
    PaymentStatus expectedStatus = new PaymentStatus();

    when(paymentProviderConfig.waitMilliseconds()).thenReturn(1.0);
    when(paymentProviderService.getPaymentStatus(ackPaymentSent)).thenReturn(expectedStatus);

    // When
    Uni<PaymentStatus> result = pollAckPaymentSentReactiveService.process(ackPaymentSent);

    // Then
    result.subscribe().with(status -> assertEquals(expectedStatus, status));
  }
}
