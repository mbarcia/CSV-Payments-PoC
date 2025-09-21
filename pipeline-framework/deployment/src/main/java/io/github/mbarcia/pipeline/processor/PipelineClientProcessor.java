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

import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.step.ConfigurableStep;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class PipelineClientProcessor {

    @BuildStep
    void generateSteps(CombinedIndexBuildItem index,
                       BuildProducer<SyntheticBeanBuildItem> beans,
                       BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        IndexView view = index.getIndex();

        for (AnnotationInstance annotationInstance : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = annotationInstance.target().asClass();

            // extract annotation values
            String stepType = annotationInstance.value("stepType") != null ? 
                annotationInstance.value("stepType").asClass().name().toString() : 
                ConfigurableStep.class.getName();
            String grpcClientValue = annotationInstance.value("grpcClient") != null ? 
                annotationInstance.value("grpcClient").asString() : stepClassInfo.name().toString().toLowerCase().replace(".", "-");
            String stubClass = annotationInstance.value("stub").asClass().name().toString();

            String stepClassName = MessageFormat.format("{0}Step", stepClassInfo.name().toString());

            // generate a subclass that implements the appropriate step interface
            try (ClassCreator cc = ClassCreator.builder()
                    .className(stepClassName)
                    .superClass(ConfigurableStep.class)
                    .build()) {

                // Add @ApplicationScoped annotation
                cc.addAnnotation(ApplicationScoped.class);
                
                // Create field for gRPC client
                FieldCreator fieldCreator = cc.getFieldCreator("grpcClient", stubClass);
                fieldCreator.addAnnotation(Inject.class);
                fieldCreator.addAnnotation(GrpcClient.class).addValue("value", grpcClientValue);

                // Add default no-arg constructor
                try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                    defaultCtor.invokeSpecialMethod(MethodDescriptor.ofConstructor(ConfigurableStep.class), defaultCtor.getThis());
                    defaultCtor.returnValue(null);
                }

                // For now, we'll pass empty bytecode - this is a simplification
                // In a real implementation, we would properly generate the bytecode
                generatedClasses.produce(new GeneratedClassBuildItem(true, stepClassName, new byte[0]));
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate step class", e);
            }
        }
    }
}