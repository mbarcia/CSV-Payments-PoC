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

import com.example.poc.common.domain.CsvPaymentsOutputFile;
import com.example.poc.common.mapper.CsvPaymentsOutputFileMapper;
import com.example.poc.common.service.GrpcReactiveServiceAdapter;
import com.example.poc.grpc.MutinyPersistCsvPaymentsOutputFileServiceGrpc;
import com.example.poc.grpc.OutputCsvFileProcessingSvc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PersistCsvPaymentsOutputFileGrpcService
        extends MutinyPersistCsvPaymentsOutputFileServiceGrpc.PersistCsvPaymentsOutputFileServiceImplBase {

    @Inject PersistCsvPaymentsOutputFileReactiveService domainService;

    @Inject CsvPaymentsOutputFileMapper mapper;

    private final GrpcReactiveServiceAdapter<
            OutputCsvFileProcessingSvc.CsvPaymentsOutputFile,
            OutputCsvFileProcessingSvc.CsvPaymentsOutputFile,
            CsvPaymentsOutputFile,
            CsvPaymentsOutputFile>
            adapter =
            new GrpcReactiveServiceAdapter<>() {

                @Override
                protected PersistCsvPaymentsOutputFileReactiveService getService() {
                    return domainService;
                }

                @Override
                protected CsvPaymentsOutputFile fromGrpc(OutputCsvFileProcessingSvc.CsvPaymentsOutputFile grpcIn) {
                    return mapper.fromGrpc(grpcIn);
                }

                @Override
                protected OutputCsvFileProcessingSvc.CsvPaymentsOutputFile toGrpc(CsvPaymentsOutputFile domainOut) {
                    return mapper.toGrpc(domainOut);
                }
            };

    @Override
    public Uni<OutputCsvFileProcessingSvc.CsvPaymentsOutputFile> remoteProcess(
            OutputCsvFileProcessingSvc.CsvPaymentsOutputFile request) {
        return adapter.remoteProcess(request);
    }
}