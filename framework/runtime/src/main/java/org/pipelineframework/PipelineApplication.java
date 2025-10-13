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
import jakarta.inject.Inject;
import org.pipelineframework.config.PipelineConfig;
import org.pipelineframework.step.*;
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
  protected PipelineExecutionService pipelineExecutionService;

  /**
   * Execute the pipeline with steps from the registry
   *
   * @param input the input Multi for the pipeline
   */
  protected void executePipeline(Multi<?> input) {
    pipelineExecutionService.executePipeline(input);
  }
  
  /**
   * Public method to execute the pipeline with a given input.
   * This method is intended to be called from external components like CLI.
   * 
   * @param input the input string for the pipeline
   */
  public void executePipelineWithInput(String input) {
      LOG.info("Processing input: " + input);
      
      // Create input Multi from the input parameter
      Multi<String> inputMulti = Multi.createFrom().item(input);
      
      // Execute the pipeline with the processed input using shared service
      pipelineExecutionService.executePipeline(inputMulti);
      LOG.info("Pipeline execution completed");
  }
}