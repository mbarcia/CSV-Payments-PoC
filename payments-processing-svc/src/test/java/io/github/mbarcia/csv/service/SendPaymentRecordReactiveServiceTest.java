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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.mbarcia.csv.common.domain.AckPaymentSent;
import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.common.mapper.SendPaymentRequestMapper;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SendPaymentRecordReactiveServiceTest {

    @Mock private PaymentProviderServiceMock paymentProviderServiceMock;

    @Mock private PaymentRecord paymentRecord;

    @InjectMocks private SendPaymentRecordReactiveService sendPaymentRecordReactiveService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExecute() {
        // Given
        AckPaymentSent expectedAck = new AckPaymentSent();

        when(paymentProviderServiceMock.sendPayment(
                        any(SendPaymentRequestMapper.SendPaymentRequest.class)))
                .thenReturn(expectedAck);

        // When
        Uni<AckPaymentSent> result = sendPaymentRecordReactiveService.process(paymentRecord);

        // Then
        result.subscribe().with(ack -> assertEquals(expectedAck, ack));
    }
}
