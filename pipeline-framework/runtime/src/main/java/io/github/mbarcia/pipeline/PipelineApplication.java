/*
 * Copyright (c) 2023-2025 Mariano Barcia
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

package io.github.mbarcia.pipeline;

import io.github.mbarcia.pipeline.config.PipelineConfig;
import io.github.mbarcia.pipeline.config.StepConfig;
import io.github.mbarcia.pipeline.step.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic pipeline application that can be extended with specific steps.
 * This class serves as a base for generated pipeline applications that include
 * all the required step implementations.
 * <p>
 * This class does not have @QuarkusMain, main() method or implement QuarkusApplication
 * to avoid being treated as an application by services that only use it as a library.
 */
@SuppressWarnings("unused")
public abstract class PipelineApplication {

  protected static final Logger LOG = LoggerFactory.getLogger(PipelineApplication.class);

  @Inject
  protected PipelineConfig pipelineConfig;

  @Inject
  protected PipelineRunner pipelineRunner;

  @Inject
  protected StepsRegistry stepsRegistry;

  /**
   * Execute the pipeline with steps from the registry
   *
   * @param input the input Multi for the pipeline
   */
  protected void executePipeline(Multi<?> input) {
    LOG.info("PIPELINE BEGINS processing");

    StopWatch watch = new StopWatch();
    watch.start();

    // Configure profiles
    pipelineConfig.defaults().retryLimit(3).debug(false);
    pipelineConfig.profile("dev", new StepConfig().retryLimit(1).debug(true));
    pipelineConfig.profile("prod", new StepConfig().retryLimit(5).retryWait(Duration.ofSeconds(1)));

    try {
      Object result = pipelineRunner.run(input);

      Multi<?> multiResult;
      if (result instanceof Multi) {
        multiResult = (Multi<?>) result;
      } else if (result instanceof Uni) {
        multiResult = ((Uni<?>) result).toMulti();
      } else {
        throw new IllegalStateException(MessageFormat.format("PipelineRunner returned unexpected type: {0}", result.getClass()));
      }

      multiResult
          .subscribe()
          .with(item -> {
                LOG.info("Processing completed.");
                watch.stop();
                LOG.info(
                    "✅ PIPELINE FINISHED processing in {} seconds",
                    watch.getTime(TimeUnit.SECONDS));
              },
              failure -> {
                LOG.error(MessageFormat.format("Error: {0}", failure.getMessage()));
                watch.stop();
                LOG.error(
                    "❌ PIPELINE FAILED after {} seconds",
                    watch.getTime(TimeUnit.SECONDS),
                    failure);
              });

    } catch (Exception e) {
      watch.stop();
      LOG.error(
          "❌ PIPELINE ABORTED after {} seconds",
          watch.getTime(TimeUnit.SECONDS),
          e);
    }
  }
}