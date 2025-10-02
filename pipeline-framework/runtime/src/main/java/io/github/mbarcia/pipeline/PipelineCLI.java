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

import io.quarkus.runtime.QuarkusApplication;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI client app for the pipeline runner.
 */
@Command(name = "pipeline", mixinStandardHelpOptions = true, version = "0.9.0", 
         description = "Pipeline execution CLI")
public class PipelineCLI extends PipelineApplication implements QuarkusApplication {

    @Inject
    PipelineRunner pipelineRunner;

    @Option(names = {"-i", "--input"}, description = "Input value for the pipeline", required = true)
    String input;

    @Override
    public int run(String... args) throws Exception {
        // Parse the command line arguments using picocli to populate the @Option fields
        int exitCode = new CommandLine(this).execute(args);
        
        // If parsing was successful and the @Option fields are populated, execute the pipeline
        if (exitCode == 0 && input != null) {
            System.out.println("Processing input: " + input);
            
            // Create input Multi from the input parameter
            Multi<String> inputMulti = Multi.createFrom().item(input);
            
            // Execute the pipeline with the processed input using the pipeline runner that will get steps from the registry
            try {
                super.executePipeline(inputMulti);
                System.out.println("Pipeline execution completed");
                return 0; // Success exit code
            } catch (Exception e) {
                System.err.println("Error executing pipeline: " + e.getMessage());
                e.printStackTrace();
                return 1; // Error exit code
            }
        }
        
        return exitCode;
    }
}