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

import io.github.mbarcia.pipeline.GenericGrpcReactiveServiceAdapter;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.*;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

// Helper class to store step information
class StepInfo {
    final String stepClassName;
    final String stepType;
    
    StepInfo(String stepClassName, String stepType) {
        this.stepClassName = stepClassName;
        this.stepType = stepType;
    }
}

public class PipelineProcessor {

    public static final String REMOTE_PROCESS_METHOD = "remoteProcess";
    public static final String UNI = "io.smallrye.mutiny.Uni";
    public static final String MULTI = "io.smallrye.mutiny.Multi";

    @BuildStep
    void generateAdapters(CombinedIndexBuildItem index,
                          BuildProducer<SyntheticBeanBuildItem> beans,
                          BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        IndexView view = index.getIndex();

        // Collect all step classes for application generation
        List<StepInfo> stepInfos = new ArrayList<>();

        for (AnnotationInstance ann : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = ann.target().asClass();

            // extract annotation values
            String stubName = ann.value("grpcStub").asClass().name().toString();
            String serviceName = stepClassInfo.name().toString();
            String inMapperName = ann.value("inboundMapper").asClass().name().toString();
            String outMapperName = ann.value("outboundMapper").asClass().name().toString();
            String grpcClientValue = ann.value("grpcClient").asString();
            String stepType = ann.value("stepType").asClass().name().toString();
            String inputType = ann.value("inputType").asClass().name().toString();
            String outputType = ann.value("outputType").asClass().name().toString();
            
            // Get backend type, defaulting to GenericGrpcReactiveServiceAdapter
            String backendType = ann.value("backendType") != null ? 
                ann.value("backendType").asClass().name().toString() : 
                GenericGrpcReactiveServiceAdapter.class.getName();

            // Generate gRPC adapter class (server-side) - the service endpoint
            generateGrpcAdapterClass(generatedClasses, stepClassInfo, backendType, inMapperName, outMapperName, serviceName);
            
            // Generate step class (client-side) - the pipeline step
            generateStepClass(generatedClasses, stepClassInfo, stubName, stepType, grpcClientValue, inputType, outputType);
            
            // Store step info for application generation
            stepInfos.add(new StepInfo(
                stepClassInfo.name().toString() + "Step",
                stepType
            ));
        }
        
        // Generate the pipeline application class if there are any steps
        if (!stepInfos.isEmpty()) {
            generatePipelineApplicationClass(generatedClasses, stepInfos);
        }
    }

    private static void generateGrpcAdapterClass(BuildProducer<GeneratedClassBuildItem> generatedClasses, ClassInfo stepClassInfo, String backendType, String inMapperName, String outMapperName, String serviceName) {
        // Generate a subclass of the specified backend adapter with @GrpcService
        String adapterClassName = MessageFormat.format("{0}GrpcService", stepClassInfo.name().toString());

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
            throw new RuntimeException("Failed to generate gRPC adapter class", e);
        }
    }

    private static void generateStepClass(BuildProducer<GeneratedClassBuildItem> generatedClasses, ClassInfo stepClassInfo, String stubName, String stepType, String grpcClientValue, String inputType, String outputType) {
        // Generate a subclass that implements the appropriate step interface
        String stepClassName = MessageFormat.format("{0}Step", stepClassInfo.name().toString());

        try (ClassCreator cc = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClasses, true),
                stepClassName, null, stepType)) {
            
            // Add @ApplicationScoped annotation
            cc.addAnnotation(ApplicationScoped.class);

            // Create field for gRPC client
            FieldCreator fieldCreator = cc.getFieldCreator("grpcClient", stubName)
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            fieldCreator.addAnnotation(Inject.class);
            fieldCreator.addAnnotation(GrpcClient.class).addValue("value", grpcClientValue);

            // Add default no-arg constructor
            try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                defaultCtor.invokeSpecialMethod(MethodDescriptor.ofConstructor(stepType), defaultCtor.getThis());
                defaultCtor.returnValue(null);
            }
            
            // Add the appropriate method implementation based on the step type
            if (stepType.endsWith("StepOneToOne")) {
                // Generate method: applyOneToOne (I -> Uni<O>)
                try (MethodCreator method = cc.getMethodCreator("applyOneToOne", UNI, inputType)) {
                    wrapCallToRemoteService(stubName, inputType, method, fieldCreator, UNI);
                }
            } else if (stepType.endsWith("StepOneToMany")) {
                // Generate method: applyOneToMany (I -> Multi<O>)
                try (MethodCreator method = cc.getMethodCreator("applyOneToMany", MULTI, inputType)) {
                    wrapCallToRemoteService(stubName, inputType, method, fieldCreator, MULTI);
                }
            } else if (stepType.endsWith("StepManyToOne")) {
                // Generate method: applyManyToOne (Multi<I> -> Uni<O>)
                try (MethodCreator method = cc.getMethodCreator("applyManyToOne", UNI, MULTI)) {
                    wrapCallToRemoteService(stubName, inputType, method, fieldCreator, UNI);
                }
            } else if (stepType.endsWith("StepManyToMany")) {
                // Generate method: applyManyToMany (Multi<I> -> Multi<O>)
                try (MethodCreator method = cc.getMethodCreator("applyManyToMany", MULTI, MULTI)) {
                    wrapCallToRemoteService(stubName, inputType, method, fieldCreator, MULTI);
                }
            } else if (stepType.endsWith("StepSideEffect")) {
                // Generate method: applySideEffect (I -> Uni<O>)
                try (MethodCreator method = cc.getMethodCreator("applySideEffect", UNI, inputType)) {
                    wrapCallToRemoteService(stubName, inputType, method, fieldCreator, UNI);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate step class", e);
        }
    }

    private static void wrapCallToRemoteService(String stubName, String inputType, MethodCreator method, FieldCreator fieldCreator, String type) {
        // body: return stub.remoteProcess(param);
        ResultHandle param = method.getMethodParam(0);
        ResultHandle stub = method.readInstanceField(fieldCreator.getFieldDescriptor(), method.getThis());
        method.returnValue(method.invokeVirtualMethod(
            MethodDescriptor.ofMethod(stubName,
                    REMOTE_PROCESS_METHOD,
                    type,
                    inputType),
            stub, param
        ));
    }

    private static void generatePipelineApplicationClass(BuildProducer<GeneratedClassBuildItem> generatedClasses, List<StepInfo> stepInfos) {
        String applicationClassName = "io.github.mbarcia.pipeline.GeneratedPipelineApplication";

        try (ClassCreator cc = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClasses, true),
                applicationClassName, null, "io.github.mbarcia.pipeline.PipelineApplication")) {

            // Add imports and annotations would be handled by the framework
            
            // Create fields for each step
            for (int i = 0; i < stepInfos.size(); i++) {
                StepInfo stepInfo = stepInfos.get(i);
                String fieldName = "step" + i;
                
                FieldCreator fieldCreator = cc.getFieldCreator(fieldName, stepInfo.stepClassName)
                        .setModifiers(java.lang.reflect.Modifier.PRIVATE);
                fieldCreator.addAnnotation(Inject.class);
            }

            // Add default no-arg constructor
            try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                defaultCtor.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor("io.github.mbarcia.pipeline.PipelineApplication"), 
                    defaultCtor.getThis());
                defaultCtor.returnValue(null);
            }
            
            // Override processPipeline method
            try (MethodCreator processMethod = cc.getMethodCreator("processPipeline", void.class, String.class)) {
                // Create a list to hold all steps
                ResultHandle stepList = processMethod.invokeStaticMethod(
                    MethodDescriptor.ofMethod(java.util.Arrays.class, "asList", List.class, Object[].class),
                    createStepArray(processMethod, stepInfos)
                );
                
                // Call the parent's executePipeline method
                ResultHandle inputMulti = processMethod.invokeStaticMethod(
                    MethodDescriptor.ofMethod(io.smallrye.mutiny.Multi.class, "createFrom", 
                        io.smallrye.mutiny.Multi.class, Object[].class),
                    processMethod.getMethodParam(0)  // input parameter
                );
                
                processMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod("io.github.mbarcia.pipeline.PipelineApplication", "executePipeline", 
                        void.class, io.smallrye.mutiny.Multi.class, List.class),
                    processMethod.getThis(),
                    inputMulti,
                    stepList
                );
                
                processMethod.returnValue(null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pipeline application class", e);
        }
    }
    
    private static ResultHandle createStepArray(MethodCreator method, List<StepInfo> stepInfos) {
        ResultHandle stepArray = method.newArray(Object.class, stepInfos.size());
        
        for (int i = 0; i < stepInfos.size(); i++) {
            StepInfo stepInfo = stepInfos.get(i);
            String fieldName = "step" + i;
            
            ResultHandle stepField = method.readInstanceField(
                FieldDescriptor.of("io.github.mbarcia.pipeline.GeneratedPipelineApplication", fieldName, stepInfo.stepClassName), 
                method.getThis()
            );
            
            method.writeArrayValue(stepArray, i, stepField);
        }
        
        return stepArray;
    }
}