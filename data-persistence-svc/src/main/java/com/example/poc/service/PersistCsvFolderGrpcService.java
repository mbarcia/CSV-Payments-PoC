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

import com.example.poc.common.domain.CsvFolder;
import com.example.poc.common.mapper.CsvFolderMapper;
import com.example.poc.common.service.GrpcReactiveServiceAdapter;
import com.example.poc.grpc.InputCsvFileProcessingSvc;
import com.example.poc.grpc.MutinyPersistCsvFolderServiceGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class PersistCsvFolderGrpcService
        extends MutinyPersistCsvFolderServiceGrpc.PersistCsvFolderServiceImplBase {

    @Inject PersistCsvFolderReactiveService domainService;

    @Inject CsvFolderMapper mapper;

    private final GrpcReactiveServiceAdapter<
            InputCsvFileProcessingSvc.CsvFolder,
            InputCsvFileProcessingSvc.CsvFolder,
            CsvFolder,
            CsvFolder>
            adapter =
            new GrpcReactiveServiceAdapter<>() {

                @Override
                protected PersistCsvFolderReactiveService getService() {
                    return domainService;
                }

                @Override
                protected CsvFolder fromGrpc(InputCsvFileProcessingSvc.CsvFolder grpcIn) {
                    return mapper.fromGrpc(grpcIn);
                }

                @Override
                protected InputCsvFileProcessingSvc.CsvFolder toGrpc(CsvFolder domainOut) {
                    return mapper.toGrpc(domainOut);
                }
            };

    @Override
    public Uni<InputCsvFileProcessingSvc.CsvFolder> remoteProcess(
            InputCsvFileProcessingSvc.CsvFolder request) {
        return adapter.remoteProcess(request);
    }
}