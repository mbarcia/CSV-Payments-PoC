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
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.pipelineframework.config.PipelineConfig;
import org.jboss.logging.Logger;

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

  /**
   * Execute the pipeline with a given input.
   * This method is used by PipelineApplication.
   *
   * @param input the input Multi for the pipeline
   */
  public Multi<?> executePipeline(Multi<?> input) {

	  return Multi.createFrom().deferred(() -> {
      // This code is executed at subscription time
      StopWatch watch = new StopWatch();
      Object result = pipelineRunner.run(input);

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
}