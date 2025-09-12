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

import io.github.mbarcia.csv.common.domain.PaymentRecord;
import io.github.mbarcia.csv.common.mapper.PaymentRecordMapper;
import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.csv.grpc.MutinyPersistPaymentRecordServiceGrpc;
import io.github.mbarcia.pipeline.grpc.GrpcReactiveServiceAdapter;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PersistPaymentRecordGrpcService
        extends MutinyPersistPaymentRecordServiceGrpc.PersistPaymentRecordServiceImplBase {

    @Inject PersistPaymentRecordReactiveService domainService;

    @Inject PaymentRecordMapper paymentStatusMapper;

    private final GrpcReactiveServiceAdapter<
            InputCsvFileProcessingSvc.PaymentRecord,
            InputCsvFileProcessingSvc.PaymentRecord,
            PaymentRecord,
            PaymentRecord>
            adapter =
            new GrpcReactiveServiceAdapter<>() {

                @Override
                protected PersistPaymentRecordReactiveService getService() {
                    return domainService;
                }

                @Override
                protected PaymentRecord fromGrpc(InputCsvFileProcessingSvc.PaymentRecord grpcIn) {
                    return paymentStatusMapper.fromGrpc(grpcIn);
                }

                @Override
                protected InputCsvFileProcessingSvc.PaymentRecord toGrpc(PaymentRecord domainOut) {
                    return paymentStatusMapper.toGrpc(domainOut);
                }
            };

    @Override
    public Uni<InputCsvFileProcessingSvc.PaymentRecord> remoteProcess(
            InputCsvFileProcessingSvc.PaymentRecord request) {
        return adapter.remoteProcess(request);
    }
}
