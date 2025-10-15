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

package org.pipelineframework.processor;

import static org.pipelineframework.processor.PipelineStepProcessor.GRPC_SERVICE_SUFFIX;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.pipelineframework.config.PipelineBuildTimeConfig;

public class StepServerRegistrar {

    private static final String FEATURE_NAME = "pipelineframework-services";
    private static final Logger LOG = Logger.getLogger(StepServerRegistrar.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void registerGeneratedServices(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
                                  PipelineBuildTimeConfig config,
                                  CombinedIndexBuildItem combinedIndex) {

        if (config.generateCli()) return;

        IndexView index = combinedIndex.getIndex();

        // Find all classes ending with "GrpcService" - these need explicit registration
        index.getKnownClasses().stream()
            .filter(ci -> ci.name().toString().endsWith(GRPC_SERVICE_SUFFIX))
            .forEach(ci -> {
                LOG.infof("Registering gRPC service: %s", ci.name());
                additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(ci.name().toString()));
            });
    }
}
