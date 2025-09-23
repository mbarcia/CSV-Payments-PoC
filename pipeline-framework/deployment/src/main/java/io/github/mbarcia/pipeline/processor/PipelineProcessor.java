/*
 * Copyright © 2023-2025 Mariano Barcia
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

import io.github.mbarcia.pipeline.GenericGrpcReactiveServiceAdapter;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class PipelineProcessor {

    @BuildStep
    void generateAdapters(CombinedIndexBuildItem index,
                          BuildProducer<SyntheticBeanBuildItem> beans,
                          BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        IndexView view = index.getIndex();

        for (AnnotationInstance ann : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = ann.target().asClass();

            // extract annotation values
            String stubName = ann.value("stub").asClass().name().toString();
            String serviceName = stepClassInfo.name().toString();
            String inMapperName = ann.value("inboundMapper").asClass().name().toString();
            String outMapperName = ann.value("outboundMapper").asClass().name().toString();
            
            // Get backend type, defaulting to GenericGrpcReactiveServiceAdapter
            String backendType = ann.value("backendType") != null ? 
                ann.value("backendType").asClass().name().toString() : 
                GenericGrpcReactiveServiceAdapter.class.getName();

            String adapterClassName = MessageFormat.format("{0}GrpcService", stepClassInfo.name().toString());

            // generate a subclass of the specified backend adapter with @GrpcService
            try (ClassCreator cc = new ClassCreator(
                    new GeneratedClassGizmoAdaptor(generatedClasses, true),
                    adapterClassName, null, backendType)) {

                // Add @GrpcService annotation to the generated class
                cc.addAnnotation(GrpcService.class);
                
                // Create fields with @Inject annotations
                cc.getFieldCreator("inboundMapper", inMapperName)
                        .setModifiers(java.lang.reflect.Modifier.PRIVATE)
                        .addAnnotation(Inject.class);
                        
                cc.getFieldCreator("outboundMapper", outMapperName)
                        .setModifiers(java.lang.reflect.Modifier.PRIVATE)
                        .addAnnotation(Inject.class);
                        
                cc.getFieldCreator("service", serviceName)
                        .setModifiers(java.lang.reflect.Modifier.PRIVATE)
                        .addAnnotation(Inject.class);
                        
                cc.getFieldCreator("persistenceManager", PersistenceManager.class.getName())
                        .setModifiers(java.lang.reflect.Modifier.PRIVATE)
                        .addAnnotation(Inject.class);

                // Add default no-arg constructor
                try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                    defaultCtor.invokeSpecialMethod(MethodDescriptor.ofConstructor(backendType), defaultCtor.getThis());
                    defaultCtor.returnValue(null);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate adapter class", e);
            }
        }
    }
}