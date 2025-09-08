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

import io.smallrye.mutiny.Uni;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Truly generic pipeline service that processes data through a series of steps.
 * This implementation is completely generic and can be used for any processing workflow.
 * <p>
 * The pipeline provides:
 * - Common error handling infrastructure
 * - Retry logic with exponential backoff
 * - Threading management
 * - Step chaining
 * <p>
 * Each step is responsible for its own business logic and can leverage the
 * common infrastructure provided by the pipeline.
 * 
 * @param <IN> Input type for the entire pipeline
 * @param <OUT> Output type for the entire pipeline
 */
public class GenericPipelineService<IN, OUT> implements Pipeline<IN, OUT> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericPipelineService.class);
    
    private final Executor executor;
    private final ProcessPipelineConfig config;
    private final List<PipelineStep<?, ?>> steps;

    public GenericPipelineService(
            Executor executor,
            ProcessPipelineConfig config,
            List<PipelineStep<?, ?>> steps) {
        this.executor = executor;
        this.config = config;
        this.steps = steps;
    }

    @Override
    public Uni<OUT> process(IN input) {
        LOG.info("🧵 Processing input on thread: {}", Thread.currentThread());
        
        // Process through the chain of steps
        return processThroughChain(input)
            .onFailure()
            .invoke(e -> LOG.error("Error processing pipeline", e));
    }

    @SuppressWarnings("unchecked")
    private Uni<OUT> processThroughChain(IN input) {
        // Start with the input
        Uni<Object> currentUni = Uni.createFrom()
            .item((Object) input)
            .runSubscriptionOn(executor);
            
        // Chain all steps together
        for (int i = 0; i < steps.size(); i++) {
            PipelineStep<Object, Object> step = (PipelineStep<Object, Object>) steps.get(i);
            String stepName = MessageFormat.format("Step {0}", i + 1);
            
            currentUni = currentUni.chain(obj -> executeStep(step, obj, stepName));
        }
        
        return (Uni<OUT>) currentUni;
    }

    private <T, R> Uni<R> executeStep(PipelineStep<T, R> step, T input, String stepName) {
        LOG.debug("Executing {}", stepName);
        
        Uni<R> result = Uni.createFrom()
            .item(input)
            .runSubscriptionOn(executor)
            .flatMap(step::execute);
            
        // Apply retry logic if configured
        if (step.getExecutionConfig().enableRetry()) {
            result = result
                .onFailure()
                .retry()
                .withBackOff(java.time.Duration.ofMillis(config.getInitialRetryDelay()),
                           java.time.Duration.ofMillis(config.getInitialRetryDelay() * 2))
                .atMost(config.getMaxRetries());
        }
        
        return result
            .onFailure()
            .invoke(e -> LOG.error("Error in {}: {}", stepName, e.getMessage(), e));
    }

}