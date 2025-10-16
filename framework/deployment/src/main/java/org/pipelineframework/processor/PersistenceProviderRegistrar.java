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

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.logging.Logger;

public class PersistenceProviderRegistrar {

    private static final String FEATURE_NAME = "pipelineframework-providers";
    private static final Logger LOG = Logger.getLogger(PersistenceProviderRegistrar.class);

    /**
     * Registers this extension's feature with the Quarkus build.
     *
     * @return a FeatureBuildItem identifying the extension feature
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    /**
     * Registers the ReactivePanachePersistenceProvider if both Panache and Hibernate Reactive are present on the classpath.
     *
     * If both `io.quarkus.hibernate.reactive.panache.PanacheEntityBase` and
     * `org.hibernate.reactive.mutiny.Mutiny$SessionFactory` are available, an unremovable
     * {@code AdditionalBeanBuildItem} for
     * {@code org.pipelineframework.persistence.provider.ReactivePanachePersistenceProvider}
     * is produced; otherwise the registration is skipped.
     *
     * @param additionalBeans producer used to register additional beans for the build; may produce an
     *                        unremovable {@code AdditionalBeanBuildItem} for the persistence provider when available
     */
    @BuildStep
    void registerReactivePersistenceProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        boolean hasPanache = classAvailable("io.quarkus.hibernate.reactive.panache.PanacheEntityBase");
        boolean hasHibernateReactive = classAvailable("org.hibernate.reactive.mutiny.Mutiny$SessionFactory");

        if (hasPanache && hasHibernateReactive) {
            LOG.info("Registering ReactivePanachePersistenceProvider");
            additionalBeans.produce(
                    AdditionalBeanBuildItem.unremovableOf(
                            "org.pipelineframework.persistence.provider.ReactivePanachePersistenceProvider"
                    )
            );
        } else {
            LOG.debugf("Skipping ReactivePanachePersistenceProvider (Panache=%s, HibernateReactive=%s)",
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