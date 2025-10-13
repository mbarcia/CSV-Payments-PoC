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

import static org.pipelineframework.processor.PipelineStepProcessor.CLIENT_STEP_SUFFIX;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import java.util.List;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.pipelineframework.config.PipelineBuildTimeConfig;

public class StepClientRegistrar {

    private static final String FEATURE_NAME = "pipelineframework-steps";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void registerStepClients(BuildProducer<AdditionalBeanBuildItem> beans,
                             PipelineBuildTimeConfig config,
                             CombinedIndexBuildItem combinedIndex) {

        IndexView index = combinedIndex.getIndex();

        // Find all classes ending with "ClientStep"
        List<ClassInfo> classes = index.getKnownClasses().stream()
                .filter(ci -> ci.name().toString().endsWith(CLIENT_STEP_SUFFIX))
                .toList();

        for (ClassInfo ci : classes) {
            if (!config.generateCli()) {
                System.out.println("Skipping step (client) " + ci.name());
                // Skip to the next class instead of returning from the entire method
            } else {
                beans.produce(AdditionalBeanBuildItem.unremovableOf(ci.name().toString()));
                System.out.println("Registered step (client) " + ci.name());
            }
        }
    }
}