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

import com.example.poc.common.domain.CsvPaymentsInputFile;
import com.example.poc.common.mapper.CsvPaymentsInputFileMapper;
import com.example.poc.common.service.GrpcReactiveServiceAdapter;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.MutinyPersistCsvPaymentsInputFileServiceGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PersistCsvPaymentsInputFileGrpcService
        extends MutinyPersistCsvPaymentsInputFileServiceGrpc.PersistCsvPaymentsInputFileServiceImplBase {

    @Inject PersistCsvPaymentsInputFileReactiveService domainService;

    @Inject CsvPaymentsInputFileMapper mapper;

    private final GrpcReactiveServiceAdapter<
            InputCsvFileProcessingSvc.CsvPaymentsInputFile,
            InputCsvFileProcessingSvc.CsvPaymentsInputFile,
            CsvPaymentsInputFile,
            CsvPaymentsInputFile>
            adapter =
            new GrpcReactiveServiceAdapter<>() {

                @Override
                protected PersistCsvPaymentsInputFileReactiveService getService() {
                    return domainService;
                }

                @Override
                protected CsvPaymentsInputFile fromGrpc(InputCsvFileProcessingSvc.CsvPaymentsInputFile grpcIn) {
                    return mapper.fromGrpc(grpcIn);
                }

                @Override
                protected InputCsvFileProcessingSvc.CsvPaymentsInputFile toGrpc(CsvPaymentsInputFile domainOut) {
                    return mapper.toGrpc(domainOut);
                }
            };

    @Override
    public Uni<InputCsvFileProcessingSvc.CsvPaymentsInputFile> remoteProcess(
            InputCsvFileProcessingSvc.CsvPaymentsInputFile request) {
        return adapter.remoteProcess(request);
    }
}