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
import io.github.mbarcia.pipeline.config.PipelineBuildTimeConfig;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
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
@SuppressWarnings("ClassCanBeRecord")
class StepInfo {
    final String stepClassName;
    final String stepType;
    
    StepInfo(String stepClassName, String stepType) {
        this.stepClassName = stepClassName;
        this.stepType = stepType;
    }
}

public class PipelineProcessor {

    private static final String FEATURE_NAME = "pipeline-framework";

    public static final String REMOTE_PROCESS_METHOD = "remoteProcess";
    public static final String UNI = "io.smallrye.mutiny.Uni";
    public static final String MULTI = "io.smallrye.mutiny.Multi";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void generateAdapters(CombinedIndexBuildItem index,
                          PipelineBuildTimeConfig config,
                          BuildProducer<SyntheticBeanBuildItem> beans,
                          BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        IndexView view = index.getIndex();

        // Collect all step classes for application generation
        List<StepInfo> stepInfos = new ArrayList<>();

        for (AnnotationInstance ann : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = ann.target().asClass();
            System.out.println("Extracting annotations from: " + stepClassInfo.name());

            // extract annotation values with null checks
            String stubName = ann.value("grpcStub") != null ? ann.value("grpcStub").asClass().name().toString() : "";
            String serviceName = stepClassInfo.name().toString();
            String inMapperName = ann.value("inboundMapper") != null ? ann.value("inboundMapper").asClass().name().toString() : "";
            String outMapperName = ann.value("outboundMapper") != null ? ann.value("outboundMapper").asClass().name().toString() : "";
            String grpcClientValue = ann.value("grpcClient") != null ? ann.value("grpcClient").asString() : "";
            String stepType = ann.value("stepType") != null ? ann.value("stepType").asClass().name().toString() : "";
            String inputType = ann.value("inputType") != null ? ann.value("inputType").asClass().name().toString() : "";
            String outputType = ann.value("outputType") != null ? ann.value("outputType").asClass().name().toString() : "";
            
            // Check if the step is local
            boolean isLocal = ann.value("local") != null ? ann.value("local").asBoolean() : false;
            
            // Get backend type, defaulting to GenericGrpcReactiveServiceAdapter
            String backendType = ann.value("backendType") != null ? 
                ann.value("backendType").asClass().name().toString() : 
                GenericGrpcReactiveServiceAdapter.class.getName();

            if (config.generateCli()) {
                if (isLocal) {
                    // Generate local step class (local service call) - the pipeline step
                    generateLocalStepClass(generatedClasses, stepClassInfo, stepType, inputType, outputType, outMapperName);
                } else {
                    // Generate step class (client-side) - the pipeline step 
                    generateGrpcClientStepClass(generatedClasses, stepClassInfo, stubName, stepType, grpcClientValue, inputType, outputType);
                }
            } else {
                if (!isLocal) {
                    // Generate gRPC adapter class (server-side) - the service endpoint
                    generateGrpcAdaptedServiceClass(generatedClasses, stepClassInfo, backendType, inMapperName, outMapperName, serviceName);
                }
                // For local steps in server mode, we don't generate gRPC adapters
            }
            // Store step info for application generation
            stepInfos.add(new StepInfo(
                stepClassInfo.name().toString() + "Step", // Always add "Step" suffix for consistency in the application class
                stepType
            ));
        }
    }

  private static void generateGrpcAdaptedServiceClass(
      BuildProducer<GeneratedClassBuildItem> generatedClasses,
      ClassInfo stepClassInfo,
      String backendType,
      String inMapperName,
      String outMapperName,
      String serviceName) {
    // Generate a subclass of the specified backend adapter with @GrpcService
    String adapterClassName =
        MessageFormat.format("{0}GrpcService", stepClassInfo.name().toString());

    System.out.println("PipelineProcessor.generateGrpcAdaptedServiceClass: " + stepClassInfo.name());

      try (ClassCreator cc =
          new ClassCreator(
              new GeneratedClassGizmoAdaptor(generatedClasses, true),
              adapterClassName,
              null,
              backendType)) {

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
          defaultCtor.invokeSpecialMethod(
              MethodDescriptor.ofConstructor(backendType), defaultCtor.getThis());
          defaultCtor.returnValue(null);
        } catch (Exception e) {
          throw new RuntimeException("Failed to generate gRPC adapter class", e);
        }
    }
  }

  private static void generateGrpcClientStepClass(
      BuildProducer<GeneratedClassBuildItem> generatedClasses,
      ClassInfo stepClassInfo,
      String stubName,
      String stepType,
      String grpcClientValue,
      String inputType,
      String outputType) {

        System.out.println("PipelineProcessor.generateGrpcClientStepClass: " + stepClassInfo.name());

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

    private static void generateLocalStepClass(
        BuildProducer<GeneratedClassBuildItem> generatedClasses,
        ClassInfo serviceClassInfo,
        String stepType,
        String inputType,
        String outputType,
        String outputMapperName) {

        System.out.println("PipelineProcessor.generateLocalStepClass: " + serviceClassInfo.name());

        // Generate a subclass that implements the appropriate step interface
        String stepClassName = MessageFormat.format("{0}Step", serviceClassInfo.name().toString());

        try (ClassCreator cc = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClasses, true),
                stepClassName, null, stepType)) {
            
            // Add @ApplicationScoped annotation
            cc.addAnnotation(ApplicationScoped.class);

            // Create field for the local service
            FieldCreator serviceField = cc.getFieldCreator("service", serviceClassInfo.name().toString())
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            serviceField.addAnnotation(Inject.class);

            // Create field for the output mapper if specified and not Void
            FieldCreator mapperField = null;
            if (!outputMapperName.equals("java.lang.Void") && !outputMapperName.isEmpty()) {
                mapperField = cc.getFieldCreator("mapper", outputMapperName)
                        .setModifiers(java.lang.reflect.Modifier.PRIVATE);
                mapperField.addAnnotation(Inject.class);
            }

            // Add default no-arg constructor
            try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                defaultCtor.invokeSpecialMethod(MethodDescriptor.ofConstructor(stepType), defaultCtor.getThis());
                defaultCtor.returnValue(null);
            }
            
            // Add the appropriate method implementation based on the step type
            if (stepType.endsWith("StepOneToOne")) {
                // Generate method: applyOneToOne (I -> Uni<O>)
                try (MethodCreator method = cc.getMethodCreator("applyOneToOne", UNI, inputType)) {
                    generateLocalStepOneToOne(method, serviceField, mapperField, serviceClassInfo, outputMapperName);
                }
            } else if (stepType.endsWith("StepOneToMany")) {
                // Generate method: applyOneToMany (I -> Multi<O>)
                try (MethodCreator method = cc.getMethodCreator("applyOneToMany", MULTI, inputType)) {
                    generateLocalStepOneToMany(method, serviceField, mapperField, serviceClassInfo, outputMapperName);
                }
            } else if (stepType.endsWith("StepManyToOne")) {
                // Generate method: applyManyToOne (Multi<I> -> Uni<O>)
                try (MethodCreator method = cc.getMethodCreator("applyManyToOne", UNI, MULTI)) {
                    generateLocalStepManyToOne(method, serviceField, mapperField, serviceClassInfo, outputMapperName);
                }
            } else if (stepType.endsWith("StepManyToMany")) {
                // Generate method: applyManyToMany (Multi<I> -> Multi<O>)
                try (MethodCreator method = cc.getMethodCreator("applyManyToMany", MULTI, MULTI)) {
                    generateLocalStepManyToMany(method, serviceField, mapperField, serviceClassInfo, outputMapperName);
                }
            } else if (stepType.endsWith("StepSideEffect")) {
                // Generate method: applySideEffect (I -> Uni<O>)
                try (MethodCreator method = cc.getMethodCreator("applySideEffect", UNI, inputType)) {
                    generateLocalStepSideEffect(method, serviceField, mapperField, serviceClassInfo, outputMapperName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate local step class", e);
        }
    }

    private static void generateLocalStepOneToMany(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        // For StepOneToMany<String, OutputType>, call the service and convert results
        ResultHandle inputParam = method.getMethodParam(0);
        ResultHandle service = method.readInstanceField(serviceField.getFieldDescriptor(), method.getThis());

        // For ProcessFolderService, call service.process(String) -> Stream<CsvPaymentsInputFile>
        ResultHandle domainStream = method.invokeVirtualMethod(
            MethodDescriptor.ofMethod(serviceClassInfo.name().toString(), "process", "java.util.stream.Stream", String.class.getName()),
            service, inputParam
        );
        
        // Convert Stream to List first, then to Multi
        ResultHandle domainList = method.invokeVirtualMethod(
            MethodDescriptor.ofMethod("java.util.stream.Stream", "toList", "java.util.List"),
            domainStream
        );
        
        // Create Multi from the list
        ResultHandle multiFromList = method.invokeStaticMethod(
            MethodDescriptor.ofMethod("io.smallrye.mutiny.Multi", "createFrom", "io.smallrye.mutiny.Multi", "java.lang.Object"),
            domainList
        );
        
        if (mapperField != null && !outputMapperName.equals("java.lang.Void") && !outputMapperName.isEmpty()) {
            // For now, we'll skip the mapping until a proper solution is implemented
            // The mapping is complex in Gizmo and requires proper lambda support
            // For now, we'll just return the domain objects as-is to allow the app to run
            method.returnValue(multiFromList);
        } else {
            // Return the Multi without mapping
            method.returnValue(multiFromList);
        }
    }

    // Placeholder methods for other step types - they can be implemented later
    private static void generateLocalStepOneToOne(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        method.throwException(RuntimeException.class, "StepOneToOne not implemented for local steps yet");
    }

    private static void generateLocalStepManyToOne(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        method.throwException(RuntimeException.class, "StepManyToOne not implemented for local steps yet");
    }

    private static void generateLocalStepManyToMany(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        method.throwException(RuntimeException.class, "StepManyToMany not implemented for local steps yet");
    }

    private static void generateLocalStepSideEffect(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        method.throwException(RuntimeException.class, "StepSideEffect not implemented for local steps yet");
    }
}