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

package io.github.mbarcia.csv.resource;

import io.github.mbarcia.csv.common.domain.AckPaymentSent;
import io.github.mbarcia.csv.common.domain.PaymentStatus;
import io.github.mbarcia.csv.common.dto.AckPaymentSentDto;
import io.github.mbarcia.csv.common.dto.PaymentStatusDto;
import io.github.mbarcia.csv.common.mapper.AckPaymentSentMapper;
import io.github.mbarcia.csv.common.mapper.PaymentStatusMapper;
import io.github.mbarcia.csv.service.PollAckPaymentSentReactiveService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/payments-processing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PollAckPaymentSentResource {

    @Inject
    PollAckPaymentSentReactiveService service;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    @Inject
    PaymentStatusMapper paymentStatusMapper;

    @POST
    @Path("/poll-ack-payment")
    public Uni<PaymentStatusDto> process(AckPaymentSentDto ackPaymentSentDto) {
        AckPaymentSent ackPaymentSent = ackPaymentSentMapper.fromDto(ackPaymentSentDto);
        Uni<PaymentStatus> paymentStatus = service.process(ackPaymentSent);
        return paymentStatus.onItem().transform(paymentStatusMapper::toDto);
    }
}