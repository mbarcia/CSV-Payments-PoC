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

package org.pipelineframework.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

/**
 * Build-time config for the Pipeline Framework extension.
 */
@ConfigMapping(prefix = "pipeline-cli")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface PipelineCliAppConfig {

    /**
     * Indicates whether the build should generate a CLI entrypoint.
     *
     * @return `true` if a CLI entrypoint will be generated, `false` otherwise.
     */
    @WithDefault("false")
    Boolean generateCli();

    /**
     * Version of The Pipeline Framework
     *
     * @return the version of The Pipeline Framework
     */
    @WithDefault("${project.version:0.9.1}")
    String version();
    
    /**
     * CLI Command Name
     * <p>
     * If not provided, defaults to an empty string (no specific command name).
     *
     * @return the CLI command name
     */
    @WithDefault("")
    Optional<String> cliName();

    /**
     * CLI Command Description
     * <p>
     * If not provided, defaults to an empty string (no description).
     *
     * @return the CLI command description
     */
    @WithDefault("")
    Optional<String> cliDescription();

    /**
     * CLI Command Version
     * <p>
     * If not provided, returns an empty Optional. Callers should use
     * cliVersion().orElse(config.version()) to get the framework version when
     * CLI version is not configured.
     *
     * @return the CLI command version
     */
    Optional<String> cliVersion();
}