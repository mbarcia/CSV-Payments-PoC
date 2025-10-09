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

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class PersistenceProviderRegistrar {

    private static final String FEATURE_NAME = "pipeline-framework-providers";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void registerReactivePersistenceProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        boolean hasPanache = classAvailable("io.quarkus.hibernate.reactive.panache.PanacheEntityBase");
        boolean hasHibernateReactive = classAvailable("org.hibernate.reactive.mutiny.Mutiny$SessionFactory");

        if (hasPanache && hasHibernateReactive) {
            System.out.println("✅ Registering ReactivePanachePersistenceProvider");
            additionalBeans.produce(
                    AdditionalBeanBuildItem.unremovableOf(
                            "io.github.mbarcia.pipeline.persistence.provider.ReactivePanachePersistenceProvider"
                    )
            );
        } else {
            System.out.printf("⚠️ Skipping ReactivePanachePersistenceProvider (Panache=%s, HibernateReactive=%s)%n",
                    hasPanache, hasHibernateReactive);
        }
    }

    private boolean classAvailable(String className) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
