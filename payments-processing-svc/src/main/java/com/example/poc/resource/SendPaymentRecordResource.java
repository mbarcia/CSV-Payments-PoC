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

package com.example.poc.resource;

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.domain.PaymentRecord;
import com.example.poc.common.dto.AckPaymentSentDto;
import com.example.poc.common.dto.PaymentRecordDto;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.mapper.PaymentRecordMapper;
import com.example.poc.service.SendPaymentRecordReactiveService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/send-payment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SendPaymentRecordResource {

    @Inject
    SendPaymentRecordReactiveService service;

    @Inject
    PaymentRecordMapper paymentRecordMapper;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    @POST
    public Uni<AckPaymentSentDto> process(PaymentRecordDto paymentRecordDto) {
        PaymentRecord paymentRecord = paymentRecordMapper.fromDto(paymentRecordDto);
        Uni<AckPaymentSent> ackPaymentSent = service.process(paymentRecord);
        return ackPaymentSent.onItem().transform(ackPaymentSentMapper::toDto);
    }
}