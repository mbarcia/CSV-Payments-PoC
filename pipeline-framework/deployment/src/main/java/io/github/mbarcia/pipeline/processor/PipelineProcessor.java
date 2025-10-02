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
import io.github.mbarcia.pipeline.StepsRegistry;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.config.PipelineBuildTimeConfig;
import io.github.mbarcia.pipeline.persistence.PersistenceManager;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    // Create a custom class writer that exports generated classes to target/gizmo for inspection
    private static ClassOutput createExportingClassOutput(BuildProducer<GeneratedClassBuildItem> generatedClasses) {
        // Create the target/gizmo directory to export classes for inspection
        Path gizmoPath = Paths.get("target", "gizmo");
        try {
            Files.createDirectories(gizmoPath);
            System.out.println("Created gizmo export directory: " + gizmoPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create gizmo export directory: " + e.getMessage());
        }

        return new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                // Write to the build system as usual
                generatedClasses.produce(new GeneratedClassBuildItem(true, name, data));
                
                // Also export to the target/gizmo directory for inspection
                try {
                    Path classPath = gizmoPath.resolve(name.replace('.', '/') + ".class");
                    Files.createDirectories(classPath.getParent());
                    try (OutputStream out = new FileOutputStream(classPath.toFile())) {
                        out.write(data);
                    }
                    System.out.println("Exported class to: " + classPath.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Failed to export class " + name + " to gizmo directory: " + e.getMessage());
                }
            }
        };
    }

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
                    generateLocalStepClass(generatedClasses, beans, stepClassInfo, stepType, inputType, outputType, outMapperName);
                } else {
                    // Generate step class (client-side) - the pipeline step 
                    generateGrpcClientStepClass(generatedClasses, beans, stepClassInfo, stubName, stepType, grpcClientValue, inputType, outputType);
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

        // Generate the StepsRegistry implementation if we have steps to register
        List<String> stepClassNames = new ArrayList<>();
        for (StepInfo stepInfo : stepInfos) {
            stepClassNames.add(stepInfo.stepClassName);
        }

        if (!stepClassNames.isEmpty()) {
            generateStepsRegistry(generatedClasses, beans, stepClassNames);
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
              createExportingClassOutput(generatedClasses),
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
      BuildProducer<SyntheticBeanBuildItem> beans,
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
                createExportingClassOutput(generatedClasses),
                stepClassName, null, Object.class.getName(), stepType)) {
            
            // Add @ApplicationScoped annotation
            cc.addAnnotation(ApplicationScoped.class);

            // Create field for gRPC client
            FieldCreator fieldCreator = cc.getFieldCreator("grpcClient", stubName)
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            fieldCreator.addAnnotation(Inject.class);
            fieldCreator.addAnnotation(GrpcClient.class).addValue("value", grpcClientValue);

            // Add default no-arg constructor
            try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                defaultCtor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class.getName()), defaultCtor.getThis());
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
        
        // Register the generated step class as a synthetic bean with all relevant types
        beans.produce(SyntheticBeanBuildItem
                .configure(DotName.createSimple(stepClassName))
                .scope(ApplicationScoped.class)
                .unremovable()
                .addType(DotName.createSimple(stepType))  // Main step interface (e.g., StepOneToMany)
                .addType(DotName.createSimple(stepClassName))  // Concrete implementation
                .addType(DotName.createSimple("io.github.mbarcia.pipeline.step.Step"))  // Base step interface
                .setRuntimeInit()
                .creator(mc -> {
                    ResultHandle instance = mc.newInstance(MethodDescriptor.ofConstructor(stepClassName));
                    mc.returnValue(instance);
                })
                .done());
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
        BuildProducer<SyntheticBeanBuildItem> beans,
        ClassInfo serviceClassInfo,
        String stepType,
        String inputType,
        String outputType,
        String outputMapperName) {

        System.out.println("PipelineProcessor.generateLocalStepClass: " + serviceClassInfo.name());

        // Generate a subclass that implements the appropriate step interface
        String stepClassName = MessageFormat.format("{0}Step", serviceClassInfo.name().toString());

        
        try (ClassCreator cc = new ClassCreator(
                createExportingClassOutput(generatedClasses),
                stepClassName,
                null,
                Object.class.getName(),
                stepType)) {
            
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
                defaultCtor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class.getName()), defaultCtor.getThis());
                defaultCtor.returnValue(null);
            }
            
            // Add the appropriate method implementation based on the step type
            if (stepType.endsWith("StepOneToMany")) {
                // Generate method: applyOneToMany (I -> Multi<O>)
                try (MethodCreator method = cc.getMethodCreator("applyOneToMany", MULTI, inputType)) {
                    generateLocalStepOneToMany(method, serviceField, mapperField, serviceClassInfo, outputMapperName);
                }
            } else if (stepType.endsWith("StepOneToOne")) {
                // Generate method: applyOneToOne (I -> Uni<O>)
                try (MethodCreator method = cc.getMethodCreator("applyOneToOne", UNI, inputType)) {
                    generateLocalStepOneToOne(method, serviceField, mapperField, serviceClassInfo, outputMapperName);
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
        
        // Register the generated step class as a synthetic bean with all relevant types
        beans.produce(SyntheticBeanBuildItem
                .configure(DotName.createSimple(stepClassName))
                .scope(ApplicationScoped.class)
                .unremovable()
                .addType(DotName.createSimple(stepType))  // Main step interface (e.g., StepOneToMany)
                .addType(DotName.createSimple(stepClassName))  // Concrete implementation
                .addType(DotName.createSimple("io.github.mbarcia.pipeline.step.Step"))  // Base step interface
                .setRuntimeInit()
                .creator(mc -> {
                    ResultHandle instance = mc.newInstance(MethodDescriptor.ofConstructor(stepClassName));
                    mc.returnValue(instance);
                })
                .done());
    }

    private static void generateLocalStepOneToMany(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        // For StepOneToMany<String, OutputType>, call the service and convert results
        ResultHandle inputParam = method.getMethodParam(0);
        ResultHandle service = method.readInstanceField(serviceField.getFieldDescriptor(), method.getThis());

        // For ProcessFolderService, call service.process(String) -> Stream<CsvPaymentsInputFile>
        ResultHandle domainStream = method.invokeVirtualMethod(
            MethodDescriptor.ofMethod(serviceClassInfo.name().toString(), "process", "java.util.stream.Stream", "java.lang.String"),
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
        
        // Return the Multi - this implements StepOneToMany.applyOneToMany correctly
        method.returnValue(multiFromList);
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
    
    private static void generateStepsRegistry(
        BuildProducer<GeneratedClassBuildItem> generatedClasses,
        BuildProducer<SyntheticBeanBuildItem> beans,
        List<String> stepClassNames) {
        
        // Generate an implementation of StepsRegistry that programmatically discovers all step instances via BeanManager
        String registryImplClassName = "io.github.mbarcia.pipeline.StepsRegistryImpl";
        
        System.out.println("Generating StepsRegistry with " + stepClassNames.size() + " steps");
        
        try (ClassCreator cc = new ClassCreator(
                createExportingClassOutput(generatedClasses),
                registryImplClassName,
                null,
                Object.class.getName(),
                StepsRegistry.class.getName())) {
            
            // Add @ApplicationScoped annotation
            cc.addAnnotation(ApplicationScoped.class);
            
            // Create a field for CDI Instance to look up all step implementations
            FieldCreator instanceField = cc.getFieldCreator("stepInstances", "jakarta.enterprise.inject.Instance")
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            instanceField.addAnnotation(Inject.class);
            
            // Add getSteps method that discovers all available step implementations
            try (MethodCreator method = cc.getMethodCreator("getSteps", "java.util.List")) {
                // Create an ArrayList to hold the discovered steps
                ResultHandle list = method.newInstance(MethodDescriptor.ofConstructor("java.util.ArrayList"));
                
                // For each step class, check if it's available and add to list if so
                for (String stepClassName : stepClassNames) {
                    // Get the specific step instance from CDI
                    ResultHandle stepProvider = method.invokeVirtualMethod(
                        MethodDescriptor.ofMethod("jakarta.enterprise.inject.Instance", "select", 
                            "jakarta.enterprise.inject.Instance", java.lang.Class.class),
                        method.readInstanceField(instanceField.getFieldDescriptor(), method.getThis()),
                        method.loadClass(stepClassName)
                    );
                    
                    // Get the step instance - check if it's available
                    ResultHandle stepInstance = method.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod("jakarta.enterprise.inject.Instance", "get", Object.class),
                        stepProvider
                    );
                    
                    // Add to list (we expect all registered steps to be available)
                    method.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod("java.util.List", "add", boolean.class, Object.class),
                        list, stepInstance
                    );
                }
                
                method.returnValue(list);
            }
            
            // Add default no-arg constructor
            try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                defaultCtor.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor("java.lang.Object"), 
                    defaultCtor.getThis()
                );
                defaultCtor.returnValue(null);
            }
            
            System.out.println("Successfully generated StepsRegistryImpl class with programmatic lookup");

        } catch (Exception e) {
            System.err.println("Failed to generate StepsRegistry implementation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate StepsRegistry implementation", e);
        }
        
        // Register the synthetic bean using proper Gizmo creator approach
        beans.produce(SyntheticBeanBuildItem
                .configure(StepsRegistry.class)
                .scope(ApplicationScoped.class)
                .unremovable()
                .addType(StepsRegistry.class)
                .creator(mc -> {
                    ResultHandle instance = mc.newInstance(MethodDescriptor.ofConstructor(registryImplClassName));
                    mc.returnValue(instance);
                })
                .done());
    }

}