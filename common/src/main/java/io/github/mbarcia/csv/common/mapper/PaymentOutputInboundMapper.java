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

package io.github.mbarcia.csv.common.mapper;

import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.github.mbarcia.pipeline.mapper.InboundMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Inbound mapper for converting gRPC PaymentOutput to domain PaymentOutput.
 */
@ApplicationScoped
public class PaymentOutputInboundMapper implements InboundMapper<PaymentStatusSvc.PaymentOutput, PaymentOutput> {

    @Inject
    PaymentOutputMapper paymentOutputMapper;

    @Override
    public PaymentOutput toDomain(PaymentStatusSvc.PaymentOutput grpcIn) {
        return paymentOutputMapper.fromGrpc(grpcIn);
    }
}