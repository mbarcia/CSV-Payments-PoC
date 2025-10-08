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

package io.github.mbarcia.orchestrator;

import io.github.mbarcia.csv.grpc.InputCsvFileProcessingSvc;
import io.github.mbarcia.pipeline.PipelineExecutionService;
import io.quarkus.runtime.QuarkusApplication;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main application class for the orchestrator service.
 * This class provides the proper Quarkus integration for the orchestrator CLI.
 */
@Command(name = "orchestrator", mixinStandardHelpOptions = true, version = "1.0.0",
         description = "CSV Payments Orchestrator Service")
@Dependent
public class OrchestratorApplication implements QuarkusApplication {

    @Option(names = {"-i", "--input"}, description = "Input value for the pipeline", required = true)
    String input;

    @Inject
    PipelineExecutionService pipelineExecutionService;

    public static void main(String[] args) {
        io.quarkus.runtime.Quarkus.run(OrchestratorApplication.class, args);
    }

    @Override
    public int run(String... args) {
        CommandLine cmd = new CommandLine(this);
        cmd.parseArgs(args);

        if (input != null) {
            executePipelineWithInput(input);
            return 0; // Success exit code
        } else {
            System.err.println("Input parameter is required");
            return 1; // Error exit code
        }
    }

    // Execute the pipeline when arguments are properly parsed
    private void executePipelineWithInput(String input) {
        InputCsvFileProcessingSvc.CsvFolder csvFolder = InputCsvFileProcessingSvc.CsvFolder.newBuilder().setPath(input).build();

        // Create input Multi from the input parameter
        Multi<InputCsvFileProcessingSvc.CsvFolder> inputMulti = Multi.createFrom().item(csvFolder);
        
        // Execute the pipeline with the processed input using injected service
        pipelineExecutionService.executePipeline(inputMulti);
        
        System.out.println("Pipeline execution completed");
    }
}