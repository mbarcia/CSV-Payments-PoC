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

package io.github.mbarcia.csv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.mbarcia.csv.common.domain.AckPaymentSent;
import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.common.mapper.AckPaymentSentMapper;
import io.github.mbarcia.csv.common.mapper.PaymentStatusMapper;
import io.github.mbarcia.csv.grpc.PaymentsProcessingSvc;
import io.smallrye.mutiny.Uni;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PollAckPaymentSentGrpcServiceTest {

  @Mock PaymentStatusMapper paymentStatusMapper;

  @Mock AckPaymentSentMapper ackPaymentSentMapper;

  @Mock PollAckPaymentSentReactiveService domainService;

  @InjectMocks private PollAckPaymentSentGrpcService service;

  private PaymentsProcessingSvc.AckPaymentSent request;
  private PaymentsProcessingSvc.PaymentStatus expectedStatus;

  @BeforeEach
  void setUp() {
    UUID conversationId = UUID.randomUUID();
    request =
        PaymentsProcessingSvc.AckPaymentSent.newBuilder()
            .setConversationId(String.valueOf(conversationId))
            .build();
    expectedStatus = PaymentsProcessingSvc.PaymentStatus.newBuilder().setStatus("SUCCESS").build();
  }

  @Test
  void testRemoteProcess_HappyPath() {

    PaymentStatus domainResponse = new PaymentStatus().setStatus("SUCCESS");
    when(domainService.process(any(AckPaymentSent.class)))
        .thenReturn(Uni.createFrom().item(domainResponse));
    when(ackPaymentSentMapper.fromGrpc(any(PaymentsProcessingSvc.AckPaymentSent.class)))
        .thenReturn(new AckPaymentSent(UUID.randomUUID()));
    when(paymentStatusMapper.toGrpc(any(PaymentStatus.class))).thenReturn(expectedStatus);

    Uni<PaymentsProcessingSvc.PaymentStatus> result = service.remoteProcess(request);

    result
        .subscribe()
        .with(
            status -> {
              assertNotNull(expectedStatus);
              assertEquals("SUCCESS", expectedStatus.getStatus());
            });
  }

  @Test
  void testRemoteProcess_UnhappyPath() {
    RuntimeException exception = new RuntimeException("Adapter error");
    when(service.remoteProcess(request)).thenReturn(Uni.createFrom().failure(exception));

    Uni<PaymentsProcessingSvc.PaymentStatus> result = service.remoteProcess(request);

    result
        .subscribe()
        .with(
            item -> fail("Should have failed"),
            failure -> assertEquals(exception.getMessage(), failure.getCause().getMessage()));
  }
}
