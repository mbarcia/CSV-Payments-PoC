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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class PipelineCLITest {

    @Test
    void testCommandLineParsing() {
        PipelineCLI cli = new PipelineCLI();
        CommandLine cmd = new CommandLine(cli);

        // Test that the command parses arguments correctly (without executing the pipeline)
        String[] args = {"--input", "test-input"};
        // Just parse the arguments - don't execute the pipeline
        cmd.parseArgs(args);

        assertEquals("test-input", cli.input, "Command should parse input value correctly");
    }

    @Test
    void testCommandLineHelp() {
        PipelineCLI cli = new PipelineCLI();
        CommandLine cmd = new CommandLine(cli);

        // Test help option
        String[] args = {"--help"};
        int exitCode = cmd.execute(args);

        assertEquals(0, exitCode, "Help command should exit with code 0");
    }

    @Test
    void testCommandLineMissingInput() {
        PipelineCLI cli = new PipelineCLI();
        CommandLine cmd = new CommandLine(cli);

        // Test validation - command should fail without required input
        String[] args = {};
        int exitCode = cmd.execute(args);

        assertNotEquals(0, exitCode, "Command should fail when required input is missing");
    }
}
