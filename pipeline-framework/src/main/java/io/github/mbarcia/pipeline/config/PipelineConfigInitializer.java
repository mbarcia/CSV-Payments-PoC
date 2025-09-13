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

package io.github.mbarcia.pipeline.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration initializer that loads static configuration into the dynamic configuration holder
 * at application startup.
 */
@ApplicationScoped
public class PipelineConfigInitializer {
    
    private static final Logger LOG = LoggerFactory.getLogger(PipelineConfigInitializer.class);
    
    @Inject
    PipelineInitialConfig staticConfig;
    
    @Inject
    PipelineDynamicConfig dynamicConfig;
    
    /**
     * Initialize the dynamic configuration with values from the static configuration
     * at application startup.
     * @param event the startup event
     */
    void onStart(@Observes StartupEvent event) {
        LOG.info("Initializing pipeline configuration");
        LOG.info("Concurrency limit records: {}", staticConfig.concurrencyLimitRecords());
        LOG.info("Max retries: {}", staticConfig.maxRetries());
        LOG.info("Initial retry delay: {}ms", staticConfig.initialRetryDelay());
        
        dynamicConfig.updateConfig(staticConfig);
        LOG.info("Pipeline configuration initialized successfully");
    }
}