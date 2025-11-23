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
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.jboss.logging.Logger;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.config.PipelineStepConfig;

/**
 * Service responsible for executing pipeline logic.
 * This service provides the shared execution logic that can be used by both
 * the PipelineApplication and the CLI app without duplicating code.
 */
@ApplicationScoped
public class PipelineExecutionService {

  private static final Logger LOG = Logger.getLogger(PipelineExecutionService.class);

  @Inject
  protected PipelineConfig pipelineConfig;

  @Inject
  protected PipelineRunner pipelineRunner;

  @Inject
  protected HealthCheckService healthCheckService;

  /**
   * Execute the configured pipeline using the provided input.
   * <p>
   * Performs a health check of dependent services before running the pipeline. If the health check fails,
   * returns a failed Multi with a RuntimeException. If the pipeline runner returns null or an unexpected
   * type, returns a failed Multi with an IllegalStateException. On success returns the Multi produced by
   * the pipeline (a Uni is converted to a Multi) with lifecycle hooks attached for timing and logging.
   *
   * @param input the input Multi supplied to the pipeline steps
   * @return the pipeline result as a Multi; if dependent services are unhealthy the Multi fails with a
   *         RuntimeException, and if the runner returns null or an unexpected type the Multi fails with
   *         an IllegalStateException
   */
  public Multi<?> executePipeline(Multi<?> input) {
    return Multi.createFrom().deferred(() -> {
      // This code is executed at subscription time
      StopWatch watch = new StopWatch();

      // Check health of dependent services before proceeding with pipeline execution
      List<Object> steps;
      try {
          steps = loadPipelineSteps();
      } catch (PipelineConfigurationException e) {
          LOG.errorf(e, "Failed to load pipeline configuration: %s", e.getMessage());
          return Multi.createFrom().failure(e);
      }

      if (!healthCheckService.checkHealthOfDependentServices(steps)) {
        return Multi.createFrom().failure(new RuntimeException("One or more dependent services are not healthy. Pipeline execution aborted after retries."));
      }

      Object result = pipelineRunner.run(input, steps);

      return switch (result) {
        case null -> Multi.createFrom().failure(new IllegalStateException(
          "PipelineRunner returned null"));
        case Multi<?> multi1 -> multi1
          .onSubscription().invoke(_ -> {
            LOG.info("PIPELINE BEGINS processing");
            watch.start();
          })
          .onCompletion().invoke(() -> {
            watch.stop();
            LOG.infof("✅ PIPELINE FINISHED processing in %s seconds", watch.getTime(TimeUnit.SECONDS));
          })
          .onFailure().invoke(failure -> {
            watch.stop();
            LOG.errorf(failure, "❌ PIPELINE FAILED after %s seconds", watch.getTime(TimeUnit.SECONDS));
          });
        case Uni<?> uni -> uni.toMulti()
          .onSubscription().invoke(_ -> {
            LOG.info("PIPELINE BEGINS processing");
            watch.start();
          })
          .onCompletion().invoke(() -> {
            watch.stop();
            LOG.infof("✅ PIPELINE FINISHED processing in %s seconds", watch.getTime(TimeUnit.SECONDS));
          })
          .onFailure().invoke(failure -> {
            watch.stop();
            LOG.errorf(failure, "❌ PIPELINE FAILED after %s seconds", watch.getTime(TimeUnit.SECONDS));
          });
        default -> Multi.createFrom().failure(new IllegalStateException(
          MessageFormat.format("PipelineRunner returned unexpected type: {0}", result.getClass().getName())
        ));
      };
    });
  }

  /**
   * Load configured pipeline steps, instantiate them as CDI-managed beans and return them in execution order.
   *
   * <p>Steps are ordered by their `order` property; entries without an `order` are treated as 0. If configuration
   * cannot be read or an error occurs while instantiating steps, an exception is thrown to indicate the failure,
   * except when no steps are configured (empty stepConfigs map), which returns an empty list.
   *
   * @return the instantiated pipeline step objects in execution order, or an empty list if no steps are configured
   * @throws PipelineConfigurationException if there are configuration or instantiation failures
   */
  private List<Object> loadPipelineSteps() {
    try {
      // Use the structured configuration mapping to get all pipeline steps
      PipelineStepConfig pipelineStepConfig = CDI.current()
        .select(PipelineStepConfig.class).get();

      Map<String, org.pipelineframework.config.PipelineStepConfig.StepConfig> stepConfigs =
        pipelineStepConfig.step();

      // Check if no steps are configured - this is a valid state that returns an empty list
      if (stepConfigs == null || stepConfigs.isEmpty()) {
        LOG.info("No pipeline steps configured, returning empty list");
        return Collections.emptyList();
      }

      // Sort the steps by their order property
      List<Map.Entry<String, org.pipelineframework.config.PipelineStepConfig.StepConfig>> sortedStepEntries =
        stepConfigs.entrySet().stream()
          .sorted(Map.Entry.comparingByValue(
            Comparator.comparingInt(config -> config.order() != null ? config.order() : 0)))
          .toList();

      List<Object> steps = new ArrayList<>();
      List<String> failedSteps = new ArrayList<>();
      for (Map.Entry<String, org.pipelineframework.config.PipelineStepConfig.StepConfig> entry : sortedStepEntries) {
        String stepClassName = entry.getKey();  // The fully qualified class name
	      Object step = createStepFromConfig(stepClassName);
        if (step != null) {
          steps.add(step);
        } else {
          failedSteps.add(stepClassName);
        }
      }

      if (!failedSteps.isEmpty()) {
        String message = String.format("Failed to instantiate %d step(s): %s",
          failedSteps.size(), String.join(", ", failedSteps));
        LOG.error(message);
        throw new PipelineConfigurationException(message);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debugf("Loaded %d pipeline steps from application properties", steps.size());
      }
      return steps;
    } catch (Exception e) {
      LOG.errorf(e, "Failed to load configuration: %s", e.getMessage());
      throw new PipelineConfigurationException("Failed to load pipeline configuration: " + e.getMessage(), e);
    }
  }

  /**
   * Instantiates a pipeline step class and returns the CDI-managed bean.
   *
   * @param stepClassName the fully qualified class name of the pipeline step
   * @return the CDI-managed instance of the step, or null if instantiation fails
   */
  private Object createStepFromConfig(String stepClassName) {
    try {
      Class<?> stepClass = Thread.currentThread().getContextClassLoader().loadClass(stepClassName);
      return io.quarkus.arc.Arc.container().instance(stepClass).get();
    } catch (Exception e) {
      LOG.errorf(e, "Failed to instantiate pipeline step: %s, error: %s", stepClassName, e.getMessage());
      return null;
    }
  }

  /**
   * Exception thrown when there are configuration issues related to pipeline setup.
   */
  public static class PipelineConfigurationException extends RuntimeException {
      public PipelineConfigurationException(String message) {
          super(message);
      }

      public PipelineConfigurationException(String message, Throwable cause) {
          super(message, cause);
      }
  }
}