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

import com.example.poc.common.domain.AckPaymentSent;
import com.example.poc.common.mapper.AckPaymentSentMapper;
import com.example.poc.common.service.GrpcReactiveServiceAdapter;
import com.example.poc.grpc.MutinyPersistAckPaymentSentServiceGrpc;
import com.example.poc.grpc.PaymentsProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PersistAckPaymentSentGrpcService
        extends MutinyPersistAckPaymentSentServiceGrpc.PersistAckPaymentSentServiceImplBase {

    @Inject PersistAckPaymentSentReactiveService domainService;

    @Inject
    AckPaymentSentMapper ackPaymentSentMapper;

    private final GrpcReactiveServiceAdapter<
            PaymentsProcessingSvc.AckPaymentSent,
            PaymentsProcessingSvc.AckPaymentSent,
            AckPaymentSent,
            AckPaymentSent>
            adapter =
            new GrpcReactiveServiceAdapter<>() {

                @Override
                protected PersistAckPaymentSentReactiveService getService() {
                    return domainService;
                }

                @Override
                protected AckPaymentSent fromGrpc(PaymentsProcessingSvc.AckPaymentSent grpcIn) {
                    return ackPaymentSentMapper.fromGrpc(grpcIn);
                }

                @Override
                protected PaymentsProcessingSvc.AckPaymentSent toGrpc(AckPaymentSent domainOut) {
                    return ackPaymentSentMapper.toGrpc(domainOut);
                }
            };

    @Override
    public Uni<PaymentsProcessingSvc.AckPaymentSent> remoteProcess(
            PaymentsProcessingSvc.AckPaymentSent request) {
        return adapter.remoteProcess(request);
    }
}
