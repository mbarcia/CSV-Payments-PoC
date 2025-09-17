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

package io.github.mbarcia.pipeline.deployment;

import io.github.mbarcia.pipeline.annotation.MapperForStep;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.grpc.deployment.BindableServiceBuildItem;
import java.util.HashMap;
import java.util.Map;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class PipelineFrameworkProcessor {

    private static final String FEATURE = "pipeline-framework";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerPipelineSteps(
            CombinedIndexBuildItem index,
            BuildProducer<BindableServiceBuildItem> bindableServices,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        IndexView indexView = index.getIndex();
        
        // Collect all mappers first
        Map<Integer, ClassInfo> stepToMapper = new HashMap<>();
        for (AnnotationInstance mapperAnnotation : indexView.getAnnotations(DotName.createSimple(MapperForStep.class.getName()))) {
            ClassInfo mapperClass = mapperAnnotation.target().asClass();
            AnnotationValue orderValue = mapperAnnotation.value("order");
            if (orderValue != null) {
                stepToMapper.put(orderValue.asInt(), mapperClass);
            }
        }

        // Process all pipeline steps
        for (AnnotationInstance stepAnnotation : indexView.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClass = stepAnnotation.target().asClass();
            
            AnnotationValue stubValue = stepAnnotation.value("stub");
            if (stubValue != null && !stubValue.asClass().name().equals(DotName.createSimple("java.lang.Void"))) {
                // Generate gRPC adapter for this step
                String generatedClassName = generateGrpcAdapter(
                    stepClass, 
                    stepAnnotation,
                    stubValue.asClass().name().toString(),
                    stepToMapper.get(stepAnnotation.value("order").asInt()),
                    generatedClasses,
                    reflectiveClasses
                );
                
                // Register the generated adapter as a bindable service
                bindableServices.produce(new BindableServiceBuildItem(DotName.createSimple(generatedClassName)));
            }
        }
    }

    private String generateGrpcAdapter(
            ClassInfo stepClass,
            AnnotationInstance stepAnnotation,
            String stubClassName,
            ClassInfo mapperClass,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        
        String stepClassName = stepClass.name().toString();
        String mapperClassName = mapperClass != null ? mapperClass.name().toString() : "java.lang.Object";
        String generatedClassName = stepClassName + "GrpcAdapter";
        
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClasses.produce(new GeneratedClassBuildItem(true, name, data));
            }
        };

        try (ClassCreator classCreator = ClassCreator.builder()
                .classOutput(classOutput)
                .className(generatedClassName)
                .superClass("io.github.mbarcia.pipeline.grpc.GenericGrpcAdapter")
                .build()) {

            // Add default constructor
            try (MethodCreator constructor = classCreator.getMethodCreator("<init>", void.class)) {
                constructor.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor("io.github.mbarcia.pipeline.grpc.GenericGrpcAdapter"),
                    constructor.getThis()
                );
                constructor.returnValue(null);
            }

            // Create fields for service, mapper and step class
            classCreator.getFieldCreator("service", "Ljava/lang/Object;").setModifiers(java.lang.reflect.Modifier.PRIVATE);
            classCreator.getFieldCreator("mapper", "Ljava/lang/Object;").setModifiers(java.lang.reflect.Modifier.PRIVATE);
            classCreator.getFieldCreator("stepClass", "Ljava/lang/Class;").setModifiers(java.lang.reflect.Modifier.PRIVATE);
            
            // Create setter methods
            try (MethodCreator setService = classCreator.getMethodCreator("setService", void.class, "Ljava/lang/Object;")) {
                setService.writeInstanceField(
                    FieldDescriptor.of(generatedClassName, "service", "Ljava/lang/Object;"),
                    setService.getThis(),
                    setService.getMethodParam(0)
                );
                setService.returnValue(null);
            }
            
            try (MethodCreator setMapper = classCreator.getMethodCreator("setMapper", void.class, "Ljava/lang/Object;")) {
                setMapper.writeInstanceField(
                    FieldDescriptor.of(generatedClassName, "mapper", "Ljava/lang/Object;"),
                    setMapper.getThis(),
                    setMapper.getMethodParam(0)
                );
                setMapper.returnValue(null);
            }
            
            try (MethodCreator setStepClass = classCreator.getMethodCreator("setStepClass", void.class, Class.class)) {
                setStepClass.writeInstanceField(
                    FieldDescriptor.of(generatedClassName, "stepClass", "Ljava/lang/Class;"),
                    setStepClass.getThis(),
                    setStepClass.getMethodParam(0)
                );
                setStepClass.returnValue(null);
            }

            // TODO: Add implementation for the abstract methods
            // This would require more complex gizmo usage to generate the actual mapping code
        }

        // Register for reflection
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(generatedClassName).methods(true).fields(true).build());
        
        return generatedClassName;
    }
}