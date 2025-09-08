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

package io.github.mbarcia.csv.common.domain;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CsvPaymentsInputStream implements CsvPaymentsInput {
    private InputStream inputStream;
    private String source;

    public CsvPaymentsInputStream(InputStream inputStream, String source) {
        this.inputStream = inputStream;
        this.source = source != null ? source : "<stream>";
    }

    @Override
    public Reader openReader() {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    @Override
    public String getSourceName() {
        return source;
    }

    @Override
    public HeaderColumnNameMappingStrategy<PaymentRecord> veryOwnStrategy() {
        HeaderColumnNameMappingStrategy<PaymentRecord> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(PaymentRecord.class); // Set the type explicitly
        return strategy;
    }
}