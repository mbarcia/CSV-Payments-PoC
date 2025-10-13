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

package org.pipelineframework;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.StepConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for executing pipeline logic.
 * This service provides the shared execution logic that can be used by both
 * the PipelineApplication and PipelineCLI without duplicating code.
 */
@ApplicationScoped
public class PipelineExecutionService {

  private static final Logger LOG = LoggerFactory.getLogger(PipelineExecutionService.class);

  @Inject
  protected PipelineConfig pipelineConfig;

  @Inject
  protected PipelineRunner pipelineRunner;

  /**
   * Execute the pipeline with a given input.
   * This method is used by both PipelineApplication and PipelineCLI.
   *
   * @param input the input Multi for the pipeline
   */
  public void executePipeline(Multi<?> input) {
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

      // Blocking invocation
      multiResult.collect().asList().await().indefinitely();
      System.exit(0);

      multiResult
        .onCompletion().invoke(() -> {
          LOG.info("Processing completed.");
          watch.stop();
          LOG.info(
              "✅ PIPELINE FINISHED processing in {} seconds",
              watch.getTime(TimeUnit.SECONDS));
        })
        .onFailure().invoke(failure -> {
          LOG.error("Error: {}", failure.getMessage());
          watch.stop();
          LOG.error(
              "❌ PIPELINE FAILED after {} seconds",
              watch.getTime(TimeUnit.SECONDS),
              failure);
        })
        .subscribe()
        .with(
          item -> LOG.debug("Processed item {}", item),
          failure -> LOG.error("❌ Unhandled pipeline error", failure)
        );
    } catch (Exception e) {
      watch.stop();
      LOG.error(
          "❌ PIPELINE ABORTED after {} seconds",
          watch.getTime(TimeUnit.SECONDS),
          e);
    }
  }
}