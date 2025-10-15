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
import org.jboss.logging.Logger;
import org.pipelineframework.config.PipelineBuildTimeConfig;

public class StepClientRegistrar {

    private static final String FEATURE_NAME = "pipelineframework-steps";
    private static final Logger LOG = Logger.getLogger(StepClientRegistrar.class);

    /**
     * Declares the build feature provided by this extension.
     *
     * @return the FeatureBuildItem for the "pipelineframework-steps" feature
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    /**
     * Registers step client classes discovered in the application index as additional unremovable beans when CLI generation is enabled.
     *
     * Scans the combined index for classes whose simple name ends with the configured client step suffix. For each matching class, if
     * pipeline CLI generation is enabled via the provided build-time configuration, produces an AdditionalBeanBuildItem for that class
     * and marks it unremovable; otherwise the class is skipped.
     *
     * @param beans a build producer used to register AdditionalBeanBuildItem instances
     * @param config pipeline build-time configuration that controls whether CLI-generated clients should be registered
     * @param combinedIndex combined Jandex index containing application classes to scan
     */
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
                LOG.debugf("Skipping step (client) %s", ci.name());
                // Skip to the next class instead of returning from the entire method
            } else {
                beans.produce(AdditionalBeanBuildItem.unremovableOf(ci.name().toString()));
                LOG.infof("Registered step (client) %s", ci.name());
            }
        }
    }
}