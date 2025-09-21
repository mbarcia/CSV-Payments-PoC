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

package io.github.mbarcia.pipeline;

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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class PipelineClientProcessor {

    @BuildStep
    void generateSteps(CombinedIndexBuildItem index,
                       BuildProducer<SyntheticBeanBuildItem> beans,
                       BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        IndexView view = index.getIndex();

        for (AnnotationInstance annotationInstance : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = annotationInstance.target().asClass();

            // extract annotation values
            DotName stepType = annotationInstance.value("step-type").asClass().name();
            DotName grpcClient = annotationInstance.value("grpc-client").asClass().name();

            String stepClassName = MessageFormat.format("{0}Step", stepClassInfo.name().toString());
            String stubClassName = MessageFormat.format("Mutiny{0}Grpc.Mutiny{0}Stub", stepClassInfo.name().toString());
            String serviceClassName = stepClassInfo.name().toString();

            // generate a subclass of ConfigurableStep, implementing the grpc stub class
            try (ClassCreator cc = ClassCreator.builder()
                    .className(stepClassName)
                    .superClass(ConfigurableStep.class)
                    .interfaces(stubClassName)
                    .build()) {

                // Create field stubClassName
                FieldCreator field = cc.getFieldCreator("serviceClassName", stubClassName);
                // Add @Inject
                field.addAnnotation(Inject.class);
                // Add @GrpcClient("example-service-client")
                field.addAnnotation(io.quarkus.grpc.GrpcClient.class)
                    .addValue("value", grpcClient); // For annotations with a single unnamed parameter like GrpcClient

//                cc.getFieldCreator("inboundMapper", inMapperName.toString())
//                        .addAnnotation(Inject.class);
//                cc.getFieldCreator("service", serviceName.toString())
//                        .addAnnotation(Inject.class);
                cc.getFieldCreator("persistenceManager", "PersistenceManager")
                        .addAnnotation(Inject.class);

                // add a no-arg ctor so Arc can create it
                try (MethodCreator ctor = cc.getMethodCreator("<init>", void.class)) {
                    ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(ConfigurableStep.class), ctor.getThis());
                    ctor.returnValue(null);
                }
            }

            // register the generated class as a CDI bean
            beans.produce(SyntheticBeanBuildItem
                    .configure(stepClassName.getClass())
                    .types(io.grpc.BindableService.class)
                    .scope(ApplicationScoped.class)
                    .done());

            generatedClasses.produce(new GeneratedClassBuildItem(true, stepClassName, null));
        }
    }

    @BuildStep
    void generateApplication(CombinedIndexBuildItem index,
                       BuildProducer<SyntheticBeanBuildItem> beans,
                       BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        IndexView view = index.getIndex();

        for (AnnotationInstance ann : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = ann.target().asClass();

            // extract annotation values
            DotName stubName = ann.value("stub").asClass().name();
            DotName serviceName = ann.value("service").asClass().name();
            DotName inMapperName = ann.value("inboundMapper").asClass().name();
            DotName outMapperName = ann.value("outboundMapper").asClass().name();

            String stepClassName = MessageFormat.format("{0}GrpcClient", stepClassInfo.name().toString());

            // generate a subclass of GenericGrpcReactiveServiceAdapter with @GrpcService
            try (ClassCreator cc = ClassCreator.builder()
                    .className(stepClassName)
                    .superClass(GenericGrpcReactiveServiceAdapter.class)
                    .build()) {

                cc.getFieldCreator("inboundMapper", inMapperName.toString())
                        .addAnnotation(Inject.class);
                cc.getFieldCreator("outboundMapper", outMapperName.toString())
                        .addAnnotation(Inject.class);
                cc.getFieldCreator("service", serviceName.toString())
                        .addAnnotation(Inject.class);
                cc.getFieldCreator("persistenceManager", "PersistenceManager")
                        .addAnnotation(Inject.class);

                // add a no-arg ctor so Arc can create it
                try (MethodCreator ctor = cc.getMethodCreator("<init>", void.class)) {
                    ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(GenericGrpcReactiveServiceAdapter.class), ctor.getThis());
                    ctor.returnValue(null);
                }
            }

            // register the generated class as a CDI bean
            beans.produce(SyntheticBeanBuildItem
                    .configure(stepClassName.getClass())
                    .types(io.grpc.BindableService.class)
                    .scope(ApplicationScoped.class)
                    .done());

            generatedClasses.produce(new GeneratedClassBuildItem(true, stepClassName, null));
        }
    }

}