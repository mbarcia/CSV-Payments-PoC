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
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PersistCsvPaymentsInputFileReactiveService implements PersistReactiveService<CsvPaymentsInputFile> {
    
    private PersistReactiveRepository<CsvPaymentsInputFile> repository;
    
    // Constructor for dependency injection
    public PersistCsvPaymentsInputFileReactiveService() {}
    
    // Constructor for testing
    public PersistCsvPaymentsInputFileReactiveService(PersistReactiveRepository<CsvPaymentsInputFile> repository) {
        this.repository = repository;
    }
    
    @Override
    public PersistReactiveRepository<CsvPaymentsInputFile> getRepository() {
        // In a real implementation, this would be injected
        // For now, we'll return the injected one or create a default
        return repository;
    }
}