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
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI client app for the pipeline runner.
 * This is a thin wrapper that handles command-line arguments and delegates
 * execution to the shared PipelineExecutionService.
 */
@Command(name = "pipeline", mixinStandardHelpOptions = true, version = "0.9.0", 
         description = "Pipeline execution CLI")
@Dependent // Mark as a CDI bean to enable injection
public class PipelineCLI implements java.util.concurrent.Callable<Integer> {

    private static final Logger LOG = Logger.getLogger(PipelineCLI.class);

    @Option(names = {"-i", "--input"}, description = "Input value for the pipeline", required = true)
    public String input;

    @Option(names = {"-a", "--args"}, description = "Additional arguments", split = " ")
    String[] additionalArgs = {};

    @Inject
    PipelineExecutionService pipelineExecutionService;

    public static void main(String[] args) {
        // Use picocli to handle command line arguments
        int exitCode = new CommandLine(new PipelineCLI()).execute(args);
        System.exit(exitCode);
    }

    // Called when the command is executed by picocli
    @Override
    public Integer call() {
        if (input != null) {
            executePipelineWithInput(input);
            return 0; // Success exit code
        } else {
            LOG.error("Input parameter is required");
            return 1; // Error exit code
        }
    }
    
    // Execute the pipeline when arguments are properly parsed
    private void executePipelineWithInput(String input) {
        LOG.infof("Processing input: %s", input);
        
        // Create input Multi from the input parameter
        Multi<String> inputMulti = Multi.createFrom().item(input);
        
        // Execute the pipeline with the processed input using injected service
        pipelineExecutionService.executePipeline(inputMulti);
        
        LOG.info("Pipeline execution completed");
    }
}