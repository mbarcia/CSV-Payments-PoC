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
import com.example.poc.repository.CsvPaymentsOutputFileRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PersistCsvPaymentsOutputFileReactiveService implements PersistReactiveService<CsvPaymentsOutputFile> {
    
    @Inject
    CsvPaymentsOutputFileRepository repository;
    
    // Constructor for testing
    public PersistCsvPaymentsOutputFileReactiveService(CsvPaymentsOutputFileRepository repository) {
        this.repository = repository;
    }
    
    // Default constructor for CDI
    public PersistCsvPaymentsOutputFileReactiveService() {}
    
    @Override
    public PanacheRepository<CsvPaymentsOutputFile> getRepository() {
        return repository;
    }

    @Override
    @WithTransaction
    public Uni<CsvPaymentsOutputFile> process(CsvPaymentsOutputFile processableObj) {
        return PersistReactiveService.super.process(processableObj);
    }
}