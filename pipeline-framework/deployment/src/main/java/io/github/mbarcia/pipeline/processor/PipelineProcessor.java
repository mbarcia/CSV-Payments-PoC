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
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
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
import org.jboss.jandex.Index;
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

    // Create a ClassOutput that also adds classes to the Jandex index
    private static ClassOutput createClassOutputForIndexing(BuildProducer<GeneratedClassBuildItem> generatedClasses, org.jboss.jandex.Indexer jandexIndexer) {
        return (name, data) -> {
            // Write to the build system as usual - this goes to target/classes
            generatedClasses.produce(new GeneratedClassBuildItem(true, name, data));

            // Also index the class with Jandex
            try {
                java.io.ByteArrayInputStream byteStream = new java.io.ByteArrayInputStream(data);
                jandexIndexer.index(byteStream);
            } catch (Exception e) {
                System.out.println("Could not index generated class " + name + ": " + e.getMessage());
            }
        };
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void generateAdapters(CombinedIndexBuildItem combinedIndex,
                          PipelineBuildTimeConfig config,
                          BuildProducer<SyntheticBeanBuildItem> beans,
                          BuildProducer<UnremovableBeanBuildItem> unremovable,
                          BuildProducer<GeneratedClassBuildItem> generatedClasses,
                          BuildProducer<GeneratedJandexIndexBuildItem> jandexIndexProducer) {

        IndexView view = combinedIndex.getIndex();
        
        // Create a separate indexer to index the generated classes directly with Jandex
        org.jboss.jandex.Indexer jandexIndexer = new org.jboss.jandex.Indexer();
        // Index the PipelineStep annotation for the indexer so we can build a complete index
        try {
            jandexIndexer.indexClass(PipelineStep.class);
        } catch (Exception e) {
            System.out.println("Could not index PipelineStep annotation: " + e.getMessage());
        }

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
            if (ann.value("outputType") != null) {
                ann.value("outputType").asClass().name();
            }

            // Check if the step is local
            boolean isLocal = ann.value("local") != null && ann.value("local").asBoolean();
            
            // Get backend type, defaulting to GenericGrpcReactiveServiceAdapter
            String backendType = ann.value("backendType") != null ? 
                ann.value("backendType").asClass().name().toString() : 
                GenericGrpcReactiveServiceAdapter.class.getName();

            if (config.generateCli()) {
                if (isLocal) {
                    // Generate local step class (local service call) - the pipeline step
                    generateLocalStepClass(generatedClasses, stepClassInfo, stepType, inputType, outMapperName, jandexIndexer);
                } else {
                    // Generate step class (client-side) - the pipeline step 
                    generateGrpcClientStepClass(generatedClasses, stepClassInfo, stubName, stepType, grpcClientValue, inputType, jandexIndexer);
                }
            } else {
                if (!isLocal) {
                    // Generate gRPC adapter class (server-side) - the service endpoint
                    generateGrpcAdaptedServiceClass(generatedClasses, stepClassInfo, backendType, inMapperName, outMapperName, serviceName, jandexIndexer);
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

        if (config.generateCli() && !stepClassNames.isEmpty()) {
            generateStepsRegistry(generatedClasses, beans, unremovable, stepClassNames, jandexIndexer);
        }
        
        // Complete the Jandex index for the generated classes and produce it
        Index finalIndex = jandexIndexer.complete();
        jandexIndexProducer.produce(new GeneratedJandexIndexBuildItem(finalIndex));
    }

  private static void generateGrpcAdaptedServiceClass(
      BuildProducer<GeneratedClassBuildItem> generatedClasses,
      ClassInfo stepClassInfo,
      String backendType,
      String inMapperName,
      String outMapperName,
      String serviceName,
      org.jboss.jandex.Indexer jandexIndexer) {
    // Generate a subclass of the specified backend adapter with @GrpcService
    String adapterClassName =
        MessageFormat.format("{0}GrpcService", stepClassInfo.name().toString());

    System.out.println("PipelineProcessor.generateGrpcAdaptedServiceClass: " + stepClassInfo.name());

      ClassOutput classOutput = createClassOutputForIndexing(generatedClasses, jandexIndexer);
      try (ClassCreator cc =
          new ClassCreator(
              classOutput,
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
      org.jboss.jandex.Indexer jandexIndexer) {

        System.out.println("PipelineProcessor.generateGrpcClientStepClass: " + stepClassInfo.name());

        // Generate a subclass that implements the appropriate step interface
        String stepClassName = MessageFormat.format("{0}Step", stepClassInfo.name().toString());

        ClassOutput classOutput = createClassOutputForIndexing(generatedClasses, jandexIndexer);
        try (ClassCreator cc = new ClassCreator(
                classOutput,
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
        String outputMapperName,
        org.jboss.jandex.Indexer jandexIndexer) {

        System.out.println("PipelineProcessor.generateLocalStepClass: " + serviceClassInfo.name());

        // Generate a subclass that implements the appropriate step interface
        String stepClassName = MessageFormat.format("{0}Step", serviceClassInfo.name().toString());

        
        ClassOutput classOutput = createClassOutputForIndexing(generatedClasses, jandexIndexer);
        try (ClassCreator cc = new ClassCreator(
                classOutput,
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
                    generateLocalStepOneToMany(method, serviceField, serviceClassInfo);
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
    }

    private static void generateLocalStepOneToMany(MethodCreator method, FieldCreator serviceField, ClassInfo serviceClassInfo) {
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
        BuildProducer<UnremovableBeanBuildItem> unremovable,
        List<String> stepClassNames,
        org.jboss.jandex.Indexer jandexIndexer) {
        
        // Generate an implementation of StepsRegistry that returns the registered steps
        String registryImplClassName = "io.github.mbarcia.pipeline.StepsRegistryImpl";
        
        System.out.println("Generating StepsRegistry with " + stepClassNames.size() + " steps");
        
        ClassOutput classOutput = createClassOutputForIndexing(generatedClasses, jandexIndexer);
        try (ClassCreator cc = new ClassCreator(
                classOutput,
                registryImplClassName,
                null,
                Object.class.getName(),
                StepsRegistry.class.getName())) {
            
            // Add @ApplicationScoped annotation
            cc.addAnnotation(ApplicationScoped.class);


            
            // Create a field for CDI Instance to look up all step implementations  
            FieldCreator instancesField = cc.getFieldCreator("instances", "jakarta.enterprise.inject.Instance")
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            instancesField.addAnnotation(Inject.class);
            
            // Add getSteps method that discovers all step implementations with null safety
            try (MethodCreator method = cc.getMethodCreator("getSteps", "java.util.List")) {
                // Create an ArrayList to hold the discovered steps
                ResultHandle list = method.newInstance(MethodDescriptor.ofConstructor("java.util.ArrayList"));
                
                // Check if the injected Instance field is null before using it
                ResultHandle instancesFieldVal = method.readInstanceField(
                    FieldDescriptor.of(registryImplClassName, "instances", "jakarta.enterprise.inject.Instance"),
                    method.getThis()
                );
                
                try (BytecodeCreator notNullBlock = method.ifNotNull(instancesFieldVal).trueBranch()) {
                    // For each step class, look it up from the injected Instance (only if not null)
                    for (String stepClassName : stepClassNames) {
                        ResultHandle stepClass = notNullBlock.loadClass(stepClassName);
                        
                        // Get the specific step instance from the CDI Instance
                        ResultHandle stepProvider = notNullBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod("jakarta.enterprise.inject.Instance", "select", 
                                "jakarta.enterprise.inject.Instance", "java.lang.Class", "[Ljava.lang.annotation.Annotation;"),
                            instancesFieldVal,  // Use the field value that we already checked isn't null
                            stepClass,
                            notNullBlock.newArray("java.lang.annotation.Annotation", 0)
                        );
                        
                        // Check if the provider is available before getting the instance
                        ResultHandle isResolvable = notNullBlock.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod("jakarta.enterprise.inject.Instance", "isResolvable", boolean.class),
                            stepProvider
                        );
                        
                        // Only add the step if it's resolvable
                        try (BytecodeCreator available = notNullBlock.ifTrue(isResolvable).trueBranch()) {
                            ResultHandle stepInstance = available.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod("jakarta.enterprise.inject.Instance", "get", Object.class),
                                stepProvider
                            );
                            
                            available.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod("java.util.List", "add", boolean.class, Object.class),
                                list, stepInstance
                            );
                        }
                    }
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
            
            System.out.println("Successfully generated StepsRegistryImpl class");

        } catch (Exception e) {
            System.err.println("Failed to generate StepsRegistry implementation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate StepsRegistry implementation", e);
        }

        // Register the synthetic bean that provides the pre-computed steps list
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
                
        // Make sure the default StepsRegistry is not used when CLI is enabled
        unremovable.produce(new UnremovableBeanBuildItem(
            new UnremovableBeanBuildItem.BeanClassNameExclusion("io.github.mbarcia.pipeline.DefaultStepsRegistry")
        ));
    }

}