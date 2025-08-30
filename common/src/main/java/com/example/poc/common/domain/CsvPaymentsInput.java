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

package com.example.poc.common.domain;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import java.io.IOException;
import java.io.Reader;

public interface CsvPaymentsInput extends AutoCloseable {
    Reader openReader() throws IOException;
    String getSourceName(); // useful for logging
    HeaderColumnNameMappingStrategy<PaymentRecord> veryOwnStrategy();
    @Override
    default void close() throws IOException {
        // Default no-op. Implementations that open resources should override.
    }
}
