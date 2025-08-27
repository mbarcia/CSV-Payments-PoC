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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;

@ApplicationScoped
@Getter
@Setter
public class ProcessFileServiceConfig {
    private volatile Long initialRetryDelay;
    private volatile Integer concurrencyLimitRecords;
    private volatile Integer maxRetries;

    @Inject
    public ProcessFileServiceConfig(ProcessFileServiceInitialConfig initialConfig) {
        this.concurrencyLimitRecords = initialConfig.concurrencyLimitRecords();
        this.maxRetries = initialConfig.maxRetries();
        this.initialRetryDelay = initialConfig.initialRetryDelay();
    }
}
