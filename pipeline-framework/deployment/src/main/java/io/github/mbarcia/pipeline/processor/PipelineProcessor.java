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
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.*;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

final class GeneratedStepsBuildItem extends SimpleBuildItem {
    private final List<String> classNames;

    public GeneratedStepsBuildItem(List<String> classNames) {
        this.classNames = classNames;
    }

    public List<String> getClassNames() {
        return classNames;
    }
}

public class PipelineProcessor {

    private static final String FEATURE_NAME = "pipeline-framework";
    
    // Common type descriptors
    private static final String UNI = "io.smallrye.mutiny.Uni";
    private static final String MULTI = "io.smallrye.mutiny.Multi";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void registerReflection(
            Optional<GeneratedStepsBuildItem> steps,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        
        if (steps.isEmpty()) {
            // No generated classes to register — skip
            return;
        }

        for (String fqcn : steps.get().getClassNames()) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(fqcn));
            additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(fqcn));
            reflectiveClasses.produce(
                    ReflectiveClassBuildItem.builder(fqcn)
                            .methods(true)
                            .fields(true)
                            .build()
            );

            System.out.println(MessageFormat.format("Registered generated class for CDI and reflection: {0}", fqcn));
        }
    }

    @BuildStep
    void generateAdapters(CombinedIndexBuildItem combinedIndex,
                          PipelineBuildTimeConfig config,
                          BuildProducer<SyntheticBeanBuildItem> beans,
                          BuildProducer<UnremovableBeanBuildItem> unremovable,
                          BuildProducer<GeneratedClassBuildItem> generatedClasses,
                          BuildProducer<AdditionalBeanBuildItem> additionalBeans,
                          BuildProducer<GeneratedStepsBuildItem> generatedSteps) {

        IndexView view = combinedIndex.getIndex();

        // Collect all step classes for application generation
        List<StepInfo> stepInfos = new ArrayList<>();
        
        for (AnnotationInstance ann : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = ann.target().asClass();
            String stepType = ann.value("stepType") != null ? ann.value("stepType").asClass().name().toString() : "";
            boolean isLocal = ann.value("local") != null && ann.value("local").asBoolean();

            // extract annotation values with null checks
            String stubName = ann.value("grpcStub") != null ? ann.value("grpcStub").asClass().name().toString() : "";
            String serviceName = stepClassInfo.name().toString();
            String inMapperName = ann.value("inboundMapper") != null ? ann.value("inboundMapper").asClass().name().toString() : "";
            String outMapperName = ann.value("outboundMapper") != null ? ann.value("outboundMapper").asClass().name().toString() : "";
            String grpcClientValue = ann.value("grpcClient") != null ? ann.value("grpcClient").asString() : "";
            String inputType = ann.value("inputType") != null ? ann.value("inputType").asClass().name().toString() : "";
            if (ann.value("outputType") != null) {
                ann.value("outputType").asClass().name();
            }

            // Get backend type, defaulting to GenericGrpcReactiveServiceAdapter
            String backendType = ann.value("backendType") != null ? 
                ann.value("backendType").asClass().name().toString() : 
                GenericGrpcReactiveServiceAdapter.class.getName();

            // Firstly, generate the server-side impls
            if (!config.generateCli() && !isLocal) {
                // Generate gRPC adapter class (server-side) - the service endpoint
                generateGrpcAdaptedServiceClass(generatedClasses, stepClassInfo, backendType, inMapperName, outMapperName, serviceName);
            }

            // Secondly, generate the client-side stubs
            if (config.generateCli() && !isLocal) {
                // Generate step class (client-side) - the pipeline step
                generateGrpcClientStepClass(generatedClasses, stepClassInfo, stubName, stepType, grpcClientValue, inputType, additionalBeans);
            }

            // Alternatively, do the "local-only" wrappers
            if (config.generateCli() && isLocal) {
                // Generate local step class (local service call) - the pipeline step
                generateLocalStepClass(
                        generatedClasses,
                        stepClassInfo,
                        stepType,
                        inputType,
                        outMapperName,
                        additionalBeans);
            }

            // Store step info for application generation
            String generatedStepClassName = MessageFormat.format("io.github.mbarcia.pipeline.generated.{0}Step", stepClassInfo.simpleName());

            stepInfos.add(new StepInfo(
                    generatedStepClassName,
                stepType
            ));
        }

        List<String> generatedStepClassNames = stepInfos.stream()
                .map(stepInfo -> stepInfo.stepClassName)
                .toList();

        // Produce custom build item for reflection registration
        generatedSteps.produce(new GeneratedStepsBuildItem(generatedStepClassNames));

        if (config.generateCli() && !generatedStepClassNames.isEmpty()) {
            generateStepsRegistry(generatedClasses, beans, unremovable, generatedStepClassNames, additionalBeans);
        }

        System.out.println("Finished pipeline processor build step");
    }

    private static void generateGrpcAdaptedServiceClass(
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            ClassInfo stepClassInfo,
            String backendType,
            String inMapperName,
            String outMapperName,
            String serviceName) {

        String pkg = "io.github.mbarcia.pipeline.generated";
        String adapterClassName = MessageFormat.format("{0}.{1}GrpcService", pkg, stepClassInfo.simpleName());
        String simpleClassName = MessageFormat.format("{0}GrpcService", stepClassInfo.simpleName());

        // Generate the source code as a string
        String sourceCode = generateGrpcAdaptedServiceSourceCode(
                pkg, 
                simpleClassName, 
                backendType, 
                inMapperName, 
                outMapperName, 
                serviceName
        );

        // Write the source file to the generated sources directory
        try {
            Path sourceDir = Paths.get("target/generated-sources/annotations");
            Path sourceFile = sourceDir.resolve(pkg.replace('.', '/') + "/" + simpleClassName + ".java");
            
            Files.createDirectories(sourceFile.getParent());
            Files.write(sourceFile, sourceCode.getBytes("UTF-8"));
            
            System.out.println(MessageFormat.format("Generated gRPC service adapter source: {0}", sourceFile));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write generated source file", e);
        }
    }

    private static String generateGrpcAdaptedServiceSourceCode(
            String pkg,
            String simpleClassName,
            String backendType,
            String inMapperName,
            String outMapperName,
            String serviceName) {
        
        StringBuilder source = new StringBuilder();
        source.append("package ").append(pkg).append(";\n\n");
        
        // Add necessary imports
        source.append("import ").append(backendType).append(";\n");
        source.append("import ").append(GrpcService.class.getName()).append(";\n");
        source.append("import ").append(ApplicationScoped.class.getName()).append(";\n");
        source.append("import ").append(Inject.class.getName()).append(";\n");
        source.append("import ").append(PersistenceManager.class.getName()).append(";\n\n");

        source.append("@GrpcService\n");
        source.append("@ApplicationScoped\n");
        source.append("public class ").append(simpleClassName).append(" extends ").append(backendType.substring(backendType.lastIndexOf('.') + 1)).append(" {\n\n");
        
        source.append("    @Inject\n");
        source.append("    private ").append(inMapperName).append(" inboundMapper;\n\n");
        
        source.append("    @Inject\n");
        source.append("    private ").append(outMapperName).append(" outboundMapper;\n\n");
        
        source.append("    @Inject\n");
        source.append("    private ").append(serviceName).append(" service;\n\n");
        
        source.append("    @Inject\n");
        source.append("    private ").append(PersistenceManager.class.getName()).append(" persistenceManager;\n\n");
        
        source.append("    public ").append(simpleClassName).append("() {\n");
        source.append("        super();\n");
        source.append("    }\n");
        source.append("}\n");

        return source.toString();
    }

    private static void generateGrpcClientStepClass(
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            ClassInfo stepClassInfo,
            String stubName,
            String stepType,
            String grpcClientValue,
            String inputType,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        System.out.println("PipelineProcessor.generateGrpcClientStepClass: " + stepClassInfo.name());

        String pkg = "io.github.mbarcia.pipeline.generated";
        String simpleName = stepClassInfo.simpleName() + "Step";
        String fqcn = pkg + "." + simpleName;

        // Generate the source code as a string
        String sourceCode = generateGrpcClientStepSourceCode(
                pkg, 
                simpleName, 
                stepType, 
                stubName, 
                grpcClientValue, 
                inputType
        );

        // Write the source file to the generated sources directory
        try {
            Path sourceDir = Paths.get("target/generated-sources/annotations");
            Path sourceFile = sourceDir.resolve(pkg.replace('.', '/') + "/" + simpleName + ".java");
            
            Files.createDirectories(sourceFile.getParent());
            Files.write(sourceFile, sourceCode.getBytes("UTF-8"));
            
            System.out.println(MessageFormat.format("Generated gRPC client step source: {0}", sourceFile));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write generated source file", e);
        }

        // Make the generated class an additional unremovable bean to ensure it's properly indexed
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(fqcn));
    }

    private static String generateGrpcClientStepSourceCode(
            String pkg,
            String simpleName,
            String stepType,
            String stubName,
            String grpcClientValue,
            String inputType) {
        
        StringBuilder source = new StringBuilder();
        source.append("package ").append(pkg).append(";\n\n");
        
        // Add necessary imports
        source.append("import ").append(stepType).append(";\n");
        source.append("import ").append(ApplicationScoped.class.getName()).append(";\n");
        source.append("import ").append(Inject.class.getName()).append(";\n");
        source.append("import ").append(GrpcClient.class.getName()).append(";\n");
        source.append("import ").append(UNI).append(";\n");
        source.append("import ").append(MULTI).append(";\n\n");

        source.append("@ApplicationScoped\n");
        source.append("public class ").append(simpleName).append(" implements ").append(stepType.substring(stepType.lastIndexOf('.') + 1)).append(" {\n\n");
        
        source.append("    @Inject\n");
        source.append("    @GrpcClient(\"").append(grpcClientValue).append("\")\n");
        source.append("    private ").append(stubName).append(" grpcClient;\n\n");
        
        source.append("    public ").append(simpleName).append("() {\n");
        source.append("    }\n\n");
        
        // Add the appropriate method implementation based on the step type
        if (stepType.endsWith("StepOneToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(extractGenericType(inputType)).append("> applyOneToOne(").append(inputType).append(" input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepOneToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(extractGenericType(inputType)).append("> applyOneToMany(").append(inputType).append(" input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(extractGenericType(inputType)).append("> applyManyToOne(Multi<").append(extractGenericType(inputType)).append("> input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(extractGenericType(inputType)).append("> applyManyToMany(Multi<").append(extractGenericType(inputType)).append("> input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepSideEffect")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(extractGenericType(inputType)).append("> applySideEffect(").append(inputType).append(" input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        }
        
        source.append("}\n");

        return source.toString();
    }

    private static String extractGenericType(String fullTypeName) {
        int genericStart = fullTypeName.indexOf('<');
        if (genericStart != -1) {
            return fullTypeName.substring(genericStart + 1, fullTypeName.length() - 1);
        }
        // If it's a simple type like String, just return as-is
        return fullTypeName;
    }

    private static void generateLocalStepClass(
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            ClassInfo serviceClassInfo,
            String stepType,
            String inputType,
            String outputMapperName, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        String pkg = "io.github.mbarcia.pipeline.generated";
        String simpleName = serviceClassInfo.simpleName() + "Step";
        String stepClassName = pkg + "." + simpleName;

        // Generate the source code as a string
        String sourceCode = generateLocalStepSourceCode(
                pkg, 
                simpleName, 
                stepType, 
                serviceClassInfo.name().toString(), 
                inputType, 
                outputMapperName
        );

        // Write the source file to the generated sources directory
        try {
            Path sourceDir = Paths.get("target/generated-sources/annotations");
            Path sourceFile = sourceDir.resolve(pkg.replace('.', '/') + "/" + simpleName + ".java");
            
            Files.createDirectories(sourceFile.getParent());
            Files.write(sourceFile, sourceCode.getBytes("UTF-8"));
            
            System.out.println(MessageFormat.format("Generated local step source: {0}", sourceFile));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write generated source file", e);
        }

        // Make the generated class an additional unremovable bean to ensure it's properly indexed
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(stepClassName));
    }

    private static String generateLocalStepSourceCode(
            String pkg,
            String simpleName,
            String stepType,
            String serviceClassName,
            String inputType,
            String outputMapperName) {
        
        StringBuilder source = new StringBuilder();
        source.append("package ").append(pkg).append(";\n\n");
        
        // Add necessary imports
        source.append("import ").append(stepType).append(";\n");
        source.append("import ").append(ApplicationScoped.class.getName()).append(";\n");
        source.append("import ").append(Inject.class.getName()).append(";\n");
        source.append("import ").append(UNI).append(";\n");
        source.append("import ").append(MULTI).append(";\n\n");

        source.append("@ApplicationScoped\n");
        source.append("public class ").append(simpleName).append(" implements ").append(stepType.substring(stepType.lastIndexOf('.') + 1)).append(" {\n\n");
        
        source.append("    @Inject\n");
        source.append("    private ").append(serviceClassName).append(" service;\n\n");
        
        // Create field for the output mapper if specified and not Void
        if (!outputMapperName.equals("java.lang.Void") && !outputMapperName.isEmpty() && !outputMapperName.isEmpty()) {
            source.append("    @Inject\n");
            source.append("    private ").append(outputMapperName).append(" mapper;\n\n");
        }

        source.append("    public ").append(simpleName).append("() {\n");
        source.append("    }\n\n");
        
        // Add the appropriate method implementation based on the step type
        if (stepType.endsWith("StepOneToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(extractGenericType(inputType)).append("> applyOneToMany(").append(inputType).append(" input) {\n");
            source.append("        java.util.stream.Stream<").append(extractGenericType(inputType)).append("> domainStream = service.process(input);\n");
            source.append("        java.util.List<").append(extractGenericType(inputType)).append("> domainList = domainStream.toList();\n");
            source.append("        return io.smallrye.mutiny.Multi.createFrom().items(domainList);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepOneToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(extractGenericType(inputType)).append("> applyOneToOne(").append(inputType).append(" input) {\n");
            source.append("        return service.process(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(extractGenericType(inputType)).append("> applyManyToOne(Multi<").append(extractGenericType(inputType)).append("> input) {\n");
            source.append("        throw new RuntimeException(\"StepManyToOne not supported for local steps - implement specific logic if needed\");\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(extractGenericType(inputType)).append("> applyManyToMany(Multi<").append(extractGenericType(inputType)).append("> input) {\n");
            source.append("        throw new RuntimeException(\"StepManyToMany not supported for local steps - implement specific logic if needed\");\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepSideEffect")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(extractGenericType(inputType)).append("> applySideEffect(").append(inputType).append(" input) {\n");
            source.append("        return service.process(input);\n");
            source.append("    }\n");
        }
        
        source.append("}\n");

        return source.toString();
    }


    
    private static void generateStepsRegistry(
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<SyntheticBeanBuildItem> beans,
            BuildProducer<UnremovableBeanBuildItem> unremovable,
            List<String> stepClassNames, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        
        String registryImplClassName = "io.github.mbarcia.pipeline.generated.StepsRegistryImpl";
        String pkg = "io.github.mbarcia.pipeline.generated";
        String simpleClassName = "StepsRegistryImpl";

        // Generate the source code as a string
        String sourceCode = generateStepsRegistrySourceCode(
                pkg, 
                simpleClassName, 
                stepClassNames
        );

        // Write the source file to the generated sources directory
        try {
            Path sourceDir = Paths.get("target/generated-sources/annotations");
            Path sourceFile = sourceDir.resolve(pkg.replace('.', '/') + "/" + simpleClassName + ".java");
            
            Files.createDirectories(sourceFile.getParent());
            Files.write(sourceFile, sourceCode.getBytes("UTF-8"));
            
            System.out.println(MessageFormat.format("Generated StepsRegistry source: {0}", sourceFile));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write generated source file", e);
        }

        // Register the synthetic bean that provides the pre-computed steps list
        beans.produce(SyntheticBeanBuildItem
                .configure(StepsRegistry.class)
                .scope(ApplicationScoped.class)
                .unremovable()
                .addType(StepsRegistry.class)
                .creator(mc -> {
                    // Use the generated class directly instead of creating a new instance via Gizmo
                    ResultHandle instance = mc.newInstance(MethodDescriptor.ofConstructor(registryImplClassName));
                    mc.returnValue(instance);
                })
                .done());
                
        // Make sure the default StepsRegistry is not used when CLI is enabled
        unremovable.produce(new UnremovableBeanBuildItem(
            new UnremovableBeanBuildItem.BeanClassNameExclusion("io.github.mbarcia.pipeline.DefaultStepsRegistry")
        ));

        // Make the generated class an additional unremovable bean to ensure it's properly indexed
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(registryImplClassName));

        System.out.println("Generated StepsRegistry with " + stepClassNames.size() + " steps");
    }

    private static String generateStepsRegistrySourceCode(
            String pkg,
            String simpleClassName,
            List<String> stepClassNames) {
        
        StringBuilder source = new StringBuilder();
        source.append("package ").append(pkg).append(";\n\n");
        
        // Add necessary imports
        source.append("import ").append("io.github.mbarcia.pipeline.StepsRegistry;\n");
        source.append("import ").append(ApplicationScoped.class.getName()).append(";\n");
        source.append("import ").append(Inject.class.getName()).append(";\n");
        source.append("import ").append("jakarta.enterprise.inject.Instance;\n");
        source.append("import ").append("java.util.List;\n");
        source.append("import ").append("java.util.ArrayList;\n\n");

        source.append("@ApplicationScoped\n");
        source.append("public class ").append(simpleClassName).append(" implements ").append("StepsRegistry {\n\n");
        
        source.append("    @Inject\n");
        source.append("    private Instance<Object> instances;\n\n");
        
        source.append("    public ").append(simpleClassName).append("() {\n");
        source.append("    }\n\n");
        
        source.append("    @Override\n");
        source.append("    public List<Object> getSteps() {\n");
        source.append("        List<Object> stepsList = new ArrayList<>();\n\n");
        
        if (!stepClassNames.isEmpty()) {
            source.append("        if (instances != null) {\n");
            for (String stepClassName : stepClassNames) {
                String stepSimpleClassName = stepClassName.substring(stepClassName.lastIndexOf('.') + 1);
                source.append("            Instance<").append(stepSimpleClassName).append("> stepProvider = instances.select(").append(stepSimpleClassName).append(".class);\n");
                source.append("            if (stepProvider.isResolvable()) {\n");
                source.append("                stepsList.add(stepProvider.get());\n");
                source.append("            }\n");
            }
            source.append("        }\n");
        }
        
        source.append("        return stepsList;\n");
        source.append("    }\n");
        source.append("}\n");

        return source.toString();
    }

}