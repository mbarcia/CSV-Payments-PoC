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

        System.out.println("PipelineProcessor.generateAdapters executed");

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

            if (config.generateCli()) {
                // Generate step class (client-side) - the pipeline step 
                generateStepClass(generatedClasses, stepClassInfo, stubName, stepType, grpcClientValue, inputType, outputType);
            } else {
                // Generate gRPC adapter class (server-side) - the service endpoint
                generateGrpcAdapterClass(generatedClasses, stepClassInfo, backendType, inMapperName, outMapperName, serviceName);
            }
            // Store step info for application generation
            stepInfos.add(new StepInfo(
                stepClassInfo.name().toString() + "Step",
                stepType
            ));
        }
    }
    
    @BuildStep
    void generateCliApplication(CombinedIndexBuildItem index,
                                PipelineBuildTimeConfig config,
                                BuildProducer<GeneratedClassBuildItem> generatedClasses) {
        IndexView view = index.getIndex();

        System.out.println("PipelineProcessor.generateCliApplication");

        // Collect all step classes for application generation only if CLI generation is enabled
        if (config.generateCli()) {
            System.out.println("PipelineProcessor.generateCliApplication (generate-cli=true)");

            List<StepInfo> stepInfos = new ArrayList<>();

            for (AnnotationInstance ann : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
                ClassInfo stepClassInfo = ann.target().asClass();

                // extract annotation values
                String stepType = ann.value("stepType").asClass().name().toString();

                // Store step info for application generation
                stepInfos.add(new StepInfo(
                    stepClassInfo.name().toString() + "Step",
                    stepType
                ));
            }
            
            // Generate the pipeline application class if steps exist
            if (!stepInfos.isEmpty()) {
                generatePipelineApplicationClass(generatedClasses, stepInfos);
            }
        }
    }

  private static void generateGrpcAdapterClass(
      BuildProducer<GeneratedClassBuildItem> generatedClasses,
      ClassInfo stepClassInfo,
      String backendType,
      String inMapperName,
      String outMapperName,
      String serviceName) {
    // Generate a subclass of the specified backend adapter with @GrpcService
    String adapterClassName =
        MessageFormat.format("{0}GrpcService", stepClassInfo.name().toString());

    System.out.println("PipelineProcessor.generateGrpcAdapterClass");

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

  private static void generateStepClass(
      BuildProducer<GeneratedClassBuildItem> generatedClasses,
      ClassInfo stepClassInfo,
      String stubName,
      String stepType,
      String grpcClientValue,
      String inputType,
      String outputType) {

        System.out.println("PipelineProcessor.generateStepClass");

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

        System.out.println("PipelineProcessor.generatePipelineApplicationClass");

        ClassCreator cc = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(generatedClasses, true))
                .className(applicationClassName)
                .superClass("io.github.mbarcia.pipeline.PipelineApplication")
                .build();

        try {
            // Add @QuarkusMain and @CommandLine.Command annotations to the generated class
            cc.addAnnotation("io.quarkus.runtime.annotations.QuarkusMain");
            var commandAnnotation = cc.addAnnotation("picocli.CommandLine.Command");
            commandAnnotation.addValue("name", "pipeline");
            commandAnnotation.addValue("mixinStandardHelpOptions", true);
            commandAnnotation.addValue("version", "1.0.0");
            commandAnnotation.addValue("description", "Generic pipeline application to process data through configured steps");

            // Create fields for each step
            for (int i = 0; i < stepInfos.size(); i++) {
                StepInfo stepInfo = stepInfos.get(i);
                String fieldName = "step" + i;
                
                FieldCreator fieldCreator = cc.getFieldCreator(fieldName, stepInfo.stepClassName)
                        .setModifiers(java.lang.reflect.Modifier.PRIVATE);
                fieldCreator.addAnnotation(Inject.class);
            }

            // Add CommandLine.IFactory field
            FieldCreator factoryField = cc.getFieldCreator("factory", "picocli.CommandLine$IFactory")
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            factoryField.addAnnotation(Inject.class);
            
            // Add @CommandLine.Option field for input
            FieldCreator inputField = cc.getFieldCreator("input", String.class)
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            var optionAnnotation = inputField.addAnnotation("picocli.CommandLine.Option");
            optionAnnotation.addValue("names", new String[]{"-i", "--input"});
            optionAnnotation.addValue("description", "Input to the pipeline");
            optionAnnotation.addValue("required", true);

            // Add default no-arg constructor
            try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                defaultCtor.invokeSpecialMethod(
                    MethodDescriptor.ofConstructor("io.github.mbarcia.pipeline.PipelineApplication"), 
                    defaultCtor.getThis());
                defaultCtor.returnValue(null);
            }
            
            // Implement CommandLine.Runnable method (void run() method)
            try (MethodCreator runMethod = cc.getMethodCreator("run", void.class)) {
                runMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod("io.github.mbarcia.pipeline.GeneratedPipelineApplication", "processPipeline", 
                        void.class, String.class),
                    runMethod.getThis(),
                    runMethod.readInstanceField(
                        FieldDescriptor.of("io.github.mbarcia.pipeline.GeneratedPipelineApplication", "input", String.class),
                        runMethod.getThis()
                    )
                );
                runMethod.returnValue(null);
            }
            
            // Implement QuarkusApplication run method (int run(String... args) method)
            try (MethodCreator quarkusRunMethod = cc.getMethodCreator("run", int.class, String[].class)) {
                ResultHandle cmdLine = quarkusRunMethod.newInstance(
                    MethodDescriptor.ofConstructor("picocli.CommandLine", Object.class, "picocli.CommandLine$IFactory"), 
                    quarkusRunMethod.getThis(),
                    quarkusRunMethod.readInstanceField(
                        FieldDescriptor.of("io.github.mbarcia.pipeline.GeneratedPipelineApplication", "factory", "picocli.CommandLine$IFactory"),
                        quarkusRunMethod.getThis()
                    )
                );
                
                ResultHandle result = quarkusRunMethod.invokeVirtualMethod(
                    MethodDescriptor.ofMethod("picocli.CommandLine", "execute", int.class, String[].class),
                    cmdLine,
                    quarkusRunMethod.getMethodParam(0)
                );
                
                quarkusRunMethod.returnValue(result);
            }
            
            // Add a static main method
            try (MethodCreator mainMethod = cc.getMethodCreator("main", void.class, String[].class)) {
                mainMethod.setModifiers(java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC);
                mainMethod.invokeStaticMethod(
                    MethodDescriptor.ofMethod("io.quarkus.runtime.Quarkus", "run", void.class, Class.class, String[].class),
                    mainMethod.loadClass("io.github.mbarcia.pipeline.GeneratedPipelineApplication"),
                    mainMethod.getMethodParam(0)
                );
                mainMethod.returnValue(null);
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
        } finally {
            cc.close();
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