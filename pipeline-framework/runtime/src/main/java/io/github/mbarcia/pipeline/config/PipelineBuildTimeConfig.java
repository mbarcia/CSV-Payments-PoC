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

package io.github.mbarcia.pipeline.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

/**
 * Build-time config for the Pipeline Framework extension.
 */
@ConfigMapping(prefix = "pipeline.build")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface PipelineBuildTimeConfig {

    /** Generate CLI entrypoint? */
    @WithDefault("false")
    Boolean generateCli();

    /** Version of The Pipeline Framework */
    @WithDefault("0.9.0")
    String version();
    
    /** CLI Command Name */
    @WithDefault("")
    Optional<String> cliName();

    /** CLI Command Description */
    @WithDefault("")
    Optional<String> cliDescription();

    /** CLI Command Version */
    @WithDefault("0.9.0")
    Optional<String> cliVersion();
}
