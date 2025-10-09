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

package io.github.mbarcia.pipeline.processor;

import io.github.mbarcia.pipeline.config.PipelineBuildTimeConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.IndexView;

public class StepServerRegistrar {

    private static final String BASE_PACKAGE = "io.github.mbarcia.csv.service.pipeline";

    private static final String FEATURE_NAME = "pipeline-framework-services";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void registerGeneratedGrpcServices(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
                                       PipelineBuildTimeConfig config,
                                       CombinedIndexBuildItem combinedIndex) {

        if (config.generateCli()) return;

        IndexView index = combinedIndex.getIndex();

        // Find all classes ending with "GrpcService"
        index.getKnownClasses().stream()
            .filter(ci -> ci.name().toString().startsWith(BASE_PACKAGE))
            .filter(ci -> ci.name().toString().endsWith("GrpcService"))
            .forEach(ci -> {
                System.out.println("Registering gRPC service: " + ci.name());
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(ci.name().toString()));
            });
    }
}
