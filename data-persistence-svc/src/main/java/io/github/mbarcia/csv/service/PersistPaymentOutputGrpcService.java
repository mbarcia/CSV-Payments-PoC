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

import io.github.mbarcia.csv.common.domain.PaymentOutput;
import io.github.mbarcia.csv.common.mapper.PaymentOutputMapper;
import io.github.mbarcia.csv.common.service.GrpcReactiveServiceAdapter;
import io.github.mbarcia.csv.grpc.MutinyPersistPaymentOutputServiceGrpc;
import io.github.mbarcia.csv.grpc.PaymentStatusSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PersistPaymentOutputGrpcService
        extends MutinyPersistPaymentOutputServiceGrpc.PersistPaymentOutputServiceImplBase {

    @Inject PersistPaymentOutputReactiveService domainService;

    @Inject PaymentOutputMapper mapper;

    private final GrpcReactiveServiceAdapter<
            PaymentStatusSvc.PaymentOutput,
            PaymentStatusSvc.PaymentOutput,
            PaymentOutput,
            PaymentOutput>
            adapter =
            new GrpcReactiveServiceAdapter<>() {

                @Override
                protected PersistPaymentOutputReactiveService getService() {
                    return domainService;
                }

                @Override
                protected PaymentOutput fromGrpc(PaymentStatusSvc.PaymentOutput grpcIn) {
                    return mapper.fromGrpc(grpcIn);
                }

                @Override
                protected PaymentStatusSvc.PaymentOutput toGrpc(PaymentOutput domainOut) {
                    return mapper.toGrpc(domainOut);
                }
            };

    @Override
    public Uni<PaymentStatusSvc.PaymentOutput> remoteProcess(
            PaymentStatusSvc.PaymentOutput request) {
        return adapter.remoteProcess(request);
    }
}