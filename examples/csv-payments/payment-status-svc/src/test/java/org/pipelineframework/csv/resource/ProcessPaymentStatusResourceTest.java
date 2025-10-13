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

package org.pipelineframework.csv.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pipelineframework.csv.common.domain.PaymentOutput;
import org.pipelineframework.csv.common.domain.PaymentStatus;
import org.pipelineframework.csv.common.dto.PaymentOutputDto;
import org.pipelineframework.csv.common.dto.PaymentStatusDto;
import org.pipelineframework.csv.common.mapper.PaymentOutputMapper;
import org.pipelineframework.csv.common.mapper.PaymentStatusMapper;
import org.pipelineframework.csv.service.ProcessPaymentStatusReactiveService;

class ProcessPaymentStatusResourceTest {

    @InjectMocks ProcessPaymentStatusResource resource;

    @Mock ProcessPaymentStatusReactiveService service;

    @Mock PaymentStatusMapper paymentStatusMapper;

    @Mock PaymentOutputMapper paymentOutputMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessEndpoint() {
        PaymentStatusDto requestDto = PaymentStatusDto.builder().build();
        PaymentStatus mappedStatus = new PaymentStatus();
        PaymentOutput serviceOutput = new PaymentOutput();
        PaymentOutputDto finalOutputDto = PaymentOutputDto.builder().message("Processed").build();

        when(paymentStatusMapper.fromDto(requestDto)).thenReturn(mappedStatus);
        when(service.process(mappedStatus)).thenReturn(Uni.createFrom().item(serviceOutput));
        when(paymentOutputMapper.toDto(serviceOutput)).thenReturn(finalOutputDto);

        Uni<PaymentOutputDto> resultUni = resource.process(requestDto);

        UniAssertSubscriber<PaymentOutputDto> subscriber =
                resultUni.subscribe().withSubscriber(UniAssertSubscriber.create());
        PaymentOutputDto resultDto = subscriber.awaitItem().getItem();

        assertEquals("Processed", resultDto.getMessage());
    }
}
