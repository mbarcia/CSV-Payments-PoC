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
import io.github.mbarcia.pipeline.recorder.StepsRegistryRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
    final int order;
    
    StepInfo(String stepClassName, String stepType, int order) {
        this.stepClassName = stepClassName;
        this.stepType = stepType;
        this.order = order;
    }
}

@SuppressWarnings({"StringTemplateMigration", "UnnecessaryUnicodeEscape"})
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
    void generateAdapters(CombinedIndexBuildItem combinedIndex,
                          PipelineBuildTimeConfig config,
                          BuildProducer<GeneratedClassBuildItem> generatedClasses,
                          BuildProducer<GeneratedResourceBuildItem> generatedResources,
                          BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
                          BuildSystemTargetBuildItem target) {

        IndexView view = combinedIndex.getIndex();
        
        Path genDir = target.getOutputDirectory().resolve("generated-sources/annotations");

        // Collect all step classes for application generation
        List<StepInfo> stepInfos = new ArrayList<>();
        
        for (AnnotationInstance ann : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = ann.target().asClass();
            String stepType = ann.value("stepType") != null ? ann.value("stepType").asClass().name().toString() : "";
            boolean isLocal = ann.value("local") != null && ann.value("local").asBoolean();
            
            // Extract order from annotation
            int order = ann.value("order") != null ? ann.value("order").asInt() : 0;

            // extract annotation values with null checks (dollar sign stripped)
            String stubName = ann.value("grpcStub") != null ? ann.value("grpcStub").asClass().name().toString().replace('\u0024', '.') : "";
            String serviceName = stepClassInfo.name().toString();
            String inMapperName = ann.value("inboundMapper") != null ? ann.value("inboundMapper").asClass().name().toString() : "";
            String outMapperName = ann.value("outboundMapper") != null ? ann.value("outboundMapper").asClass().name().toString() : "";
            String grpcClientValue = ann.value("grpcClient") != null ? ann.value("grpcClient").asString() : "";
            String inputType = ann.value("inputType") != null ? ann.value("inputType").asClass().name().toString() : "";
            // Get output type from annotation
            String outputType = "";
            if (ann.value("outputType") != null) {
                outputType = ann.value("outputType").asClass().name().toString();
            }
            // Get gRPC input and output types from annotation (dollar sign stripped)
            String inputGrpcType = ann.value("inputGrpcType") != null ? ann.value("inputGrpcType").asClass().name().toString().replace('\u0024', '.') : "";
            String outputGrpcType = ann.value("outputGrpcType") != null ? ann.value("outputGrpcType").asClass().name().toString().replace('\u0024', '.') : "";
            // Get backend type, defaulting to GenericGrpcReactiveServiceAdapter
            String backendType = ann.value("backendType") != null ? 
                ann.value("backendType").asClass().name().toString() : 
                GenericGrpcReactiveServiceAdapter.class.getName();

            // Firstly, generate the server-side impls
            if (!config.generateCli() && !isLocal) {
                // Extract auto-persistence setting from annotation
                boolean autoPersistenceEnabled = true; // default value
                if (ann.value("autoPersistence") != null) {
                    autoPersistenceEnabled = ann.value("autoPersistence").asBoolean();
                }
                // Generate gRPC adapter class (server-side) - the service endpoint
                generateGrpcAdaptedServiceClass(stepClassInfo, backendType, inMapperName, outMapperName, serviceName, autoPersistenceEnabled, genDir);
            }

            // Secondly, generate the client-side stubs
            if (config.generateCli() && !isLocal) {
                // Generate step class (client-side) - the pipeline step
                generateGrpcClientStepClass(stepClassInfo, stubName, stepType, grpcClientValue, inputGrpcType, outputGrpcType, genDir);
            }

            // Alternatively, do the "local-only" wrappers
            if (config.generateCli() && isLocal) {
                // Generate local step class (local service call) - the pipeline step
                generateLocalStepClass(
                        stepClassInfo,
                        stepType,
                        inputType,
                        outputType,  // Changed from outMapperName to outputType
                        genDir
                );
            }

            // Store step info for application generation - using fully qualified class name
            String originalFqcn = stepClassInfo.name().toString();
            String originalPackage = originalFqcn.substring(0, originalFqcn.lastIndexOf('.'));
            String pkg = MessageFormat.format("{0}.pipeline", originalPackage);
            String simpleName = MessageFormat.format("{0}Step", stepClassInfo.simpleName());
            String generatedStepClassName = pkg + "." + simpleName;

            stepInfos.add(new StepInfo(
                    generatedStepClassName,
                    stepType,
                    order
            ));
        }

        // Sort the stepInfos list by order before generating the StepsRegistry
        stepInfos.sort(Comparator.comparingInt(info -> info.order));
        
        List<String> generatedStepClassNames = stepInfos.stream()
                .map(stepInfo -> stepInfo.stepClassName)
                .toList();

        if (config.generateCli() && !generatedStepClassNames.isEmpty()) {
            generateStepsRegistry(generatedStepClassNames, generatedClasses, generatedResources, reflectiveClasses);
        }

        System.out.println("Finished pipeline processor build step");
    }

    private static void generateGrpcAdaptedServiceClass(
            ClassInfo stepClassInfo,
            String backendType,
            String inMapperName,
            String outMapperName,
            String serviceName,
            boolean autoPersistenceEnabled,
            Path genDir) {

        // Use the same package as the original service but with a ".pipeline" suffix
        String fqcn = stepClassInfo.name().toString();
        String originalPackage = fqcn.substring(0, fqcn.lastIndexOf('.'));
        String pkg = MessageFormat.format("{0}.pipeline", originalPackage);
        String simpleClassName = MessageFormat.format("{0}GrpcService", stepClassInfo.simpleName());

        // Generate the source code as a string
        String sourceCode = generateGrpcAdaptedServiceSourceCode(
                pkg, 
                simpleClassName, 
                backendType, 
                inMapperName, 
                outMapperName, 
                serviceName,
                autoPersistenceEnabled);  // The corresponding step class name and auto-persistence setting

        writeSourceFile(pkg, simpleClassName, sourceCode, genDir);
    }

    private static void writeSourceFile(String pkg,
                                        String simpleClassName,
                                        String sourceCode,
                                        Path genDir) {
        // Write the source file to the generated sources directory
        try {
                Path sourceFile = genDir.resolve(pkg.replace('.', '/') + "/" + simpleClassName + ".java");
                Files.createDirectories(sourceFile.getParent());
                Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
                System.out.println(MessageFormat.format("Generated gRPC service adapter source: {0}", sourceFile));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write generated source file", e);
        }
    }

    private static void registerBuildItem(String pkg,
                                        String simpleClassName,
                                        String sourceCode,
                                        BuildProducer<GeneratedClassBuildItem> generatedClasses,
                                        BuildProducer<GeneratedResourceBuildItem> generatedResources,
                                        BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        try {
                String className = pkg + "." + simpleClassName;
                byte[] classBytes = InMemoryCompiler.compile(className, sourceCode);

                generatedClasses.produce(new GeneratedClassBuildItem(
                        true, // include in runtime classloader
                        className,
                        classBytes
                ));
                generatedResources.produce(new GeneratedResourceBuildItem(
                        className.replace('.', '/') + ".class",
                        classBytes
                ));
                reflectiveClasses.produce(
                        ReflectiveClassBuildItem.builder(className)
                                .methods(true)
                                .fields(true)
                                .build()
                );

                System.out.println(MessageFormat.format("Registered class: {0}", simpleClassName));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile StepsRegistryImpl", e);
        }
    }

    private static String generateGrpcAdaptedServiceSourceCode(
            String pkg,
            String simpleClassName,
            String backendType,
            String inMapperName,
            String outMapperName,
            String serviceName,
            boolean autoPersistenceEnabled) {

        return "package " + pkg + ";\n\n" +

                // Add necessary imports
                "import " + backendType + ";\n" +
                "import " + GrpcService.class.getName() + ";\n" +
                "import " + ApplicationScoped.class.getName() + ";\n" +
                "import " + Inject.class.getName() + ";\n" +
                "import " + PersistenceManager.class.getName() + ";\n" +
                "import lombok.Getter;\n" +
                "import lombok.NoArgsConstructor;\n\n" +
                "@GrpcService\n" +
                "@ApplicationScoped\n" +
                "@NoArgsConstructor\n" +
                "@Getter\n" +
                "public class " + simpleClassName + " extends " + backendType + " {\n\n" +
                "    @Inject\n" +
                "    private " + inMapperName + " inboundMapper;\n\n" +
                "    @Inject\n" +
                "    private " + outMapperName + " outboundMapper;\n\n" +
                "    @Inject\n" +
                "    private " + serviceName + " service;\n\n" +
                "    @Inject\n" +
                "    private " + PersistenceManager.class.getName() + " persistenceManager;\n\n" +
                "    @Override\n" +
                "    protected boolean isAutoPersistenceEnabled() {\n" +
                "        return " + autoPersistenceEnabled + ";\n" +
                "    }\n" +
                "}\n";
    }

    private static void generateGrpcClientStepClass(
            ClassInfo stepClassInfo,
            String stubName,
            String stepType,
            String grpcClientValue,
            String inputGrpcType,
            String outputGrpcType,
            Path genDir) {

        System.out.println("PipelineProcessor.generateGrpcClientStepClass: " + stepClassInfo.name());

        // Use the same package as the original service but with a ".pipeline" suffix
        String originalFqcn = stepClassInfo.name().toString();
        String originalPackage = originalFqcn.substring(0, originalFqcn.lastIndexOf('.'));
        String pkg = MessageFormat.format("{0}.pipeline", originalPackage);
        String simpleName = MessageFormat.format("{0}Step", stepClassInfo.simpleName());

        // Generate the source code as a string
        String sourceCode = generateGrpcClientStepSourceCode(
                pkg, 
                simpleName, 
                stepType, 
                stubName, 
                grpcClientValue,
                inputGrpcType,
                outputGrpcType
        );

        writeSourceFile(pkg, simpleName, sourceCode, genDir);
    }

    private static String generateGrpcClientStepSourceCode(
            String pkg,
            String simpleName,
            String stepType,
            String stubName,
            String grpcClientValue,
            String inputGrpcType,
            String outputGrpcType) {
        
        StringBuilder source = new StringBuilder();
        source.append("package ").append(pkg).append(";\n\n");
        
        // Add necessary imports
        source.append("import io.github.mbarcia.pipeline.step.ConfigurableStep;\n");
        source.append("import ").append(stepType).append(";\n");
        source.append("import ").append(ApplicationScoped.class.getName()).append(";\n");
        source.append("import ").append(Inject.class.getName()).append(";\n");
        source.append("import ").append(GrpcClient.class.getName()).append(";\n");
        source.append("import ").append(UNI).append(";\n");
        source.append("import ").append(MULTI).append(";\n\n");

        // Handle cases where inputGrpcType or outputGrpcType might be empty/invalid
        String effectiveInputType = inputGrpcType != null && !inputGrpcType.isEmpty() ? inputGrpcType : "java.lang.Object";
        String effectiveOutputType = outputGrpcType != null && !outputGrpcType.isEmpty() ? outputGrpcType : "java.lang.Object";
        
        source.append("public class ").append(simpleName).append(" extends ConfigurableStep implements ").append(stepType.substring(stepType.lastIndexOf('.') + 1)).append("<").append(effectiveInputType).append(", ").append(effectiveOutputType).append("> {\n\n");
        
        source.append("    @Inject\n");
        source.append("    @GrpcClient(\"").append(grpcClientValue).append("\")\n");
        source.append("    private ").append(stubName).append(" grpcClient;\n\n");
        
        source.append("    public ").append(simpleName).append("() {\n");
        source.append("    }\n\n");
        
        // Add the appropriate method implementation based on the step type
        if (stepType.endsWith("StepOneToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(effectiveOutputType).append("> applyOneToOne(").append(effectiveInputType).append(" input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepOneToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(effectiveOutputType).append("> applyOneToMany(").append(effectiveInputType).append(" input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(effectiveOutputType).append("> applyManyToOne(Multi<").append(effectiveInputType).append("> input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(effectiveOutputType).append("> applyManyToMany(Multi<").append(effectiveInputType).append("> input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepSideEffect")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(effectiveOutputType).append("> applySideEffect(").append(effectiveInputType).append(" input) {\n");
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
            ClassInfo serviceClassInfo,
            String stepType,
            String inputType,
            String outputType,
            Path genDir) {

        // Use the same package as the original service but with a ".pipeline" suffix
        String originalServicePackage = serviceClassInfo.name().toString();
        String basePackage = originalServicePackage.substring(0, originalServicePackage.lastIndexOf('.'));
        String pkg = basePackage + ".pipeline";
        String simpleName = serviceClassInfo.simpleName() + "Step";

        // Generate the source code as a string
        String sourceCode = generateLocalStepSourceCode(
                pkg, 
                simpleName, 
                stepType, 
                serviceClassInfo.name().toString(), 
                inputType, 
                outputType
        );

        writeSourceFile(pkg, simpleName, sourceCode, genDir);
    }

    private static String generateLocalStepSourceCode(
            String pkg,
            String simpleName,
            String stepType,
            String serviceClassName,
            String inputType,
            String outputType) {
        
        StringBuilder source = new StringBuilder();
        source.append("package ").append(pkg).append(";\n\n");
        
        // Add necessary imports
        source.append("import ").append(stepType).append(";\n");
        source.append("import ").append(ApplicationScoped.class.getName()).append(";\n");
        source.append("import ").append(Inject.class.getName()).append(";\n");
        source.append("import ").append(UNI).append(";\n");
        source.append("import ").append(MULTI).append(";\n\n");

        
        source.append("public class ").append(simpleName).append(" implements ").append(stepType.substring(stepType.lastIndexOf('.') + 1)).append(" {\n\n");
        
        source.append("    @Inject\n");
        source.append("    private ").append(serviceClassName).append(" service;\n\n");
        
        // Create field for the output mapper if specified and not Void
        if (!outputType.equals("java.lang.Void") && !outputType.isEmpty()) {
            // This can be used as the output mapper if needed
            source.append("    // Output type: ").append(outputType).append("\n");
        }

        source.append("    public ").append(simpleName).append("() {\n");
        source.append("    }\n\n");
        
        // Determine the output type to use - if outputType is provided and not empty, use it, else try to extract from input
        String returnType = outputType.isEmpty() ? extractGenericType(inputType) : outputType;
        
        // Add the appropriate method implementation based on the step type
        if (stepType.endsWith("StepOneToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(returnType).append("> applyOneToMany(").append(inputType).append(" input) {\n");
            source.append("        java.util.stream.Stream<").append(returnType).append("> domainStream = service.process(input);\n");
            source.append("        java.util.List<").append(returnType).append("> domainList = domainStream.toList();\n");
            source.append("        return io.smallrye.mutiny.Multi.createFrom().items(domainList);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepOneToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(returnType).append("> applyOneToOne(").append(inputType).append(" input) {\n");
            source.append("        return service.process(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(returnType).append("> applyManyToOne(Multi<").append(extractGenericType(inputType)).append("> input) {\n");
            source.append("        throw new RuntimeException(\"StepManyToOne not supported for local steps - implement specific logic if needed\");\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(returnType).append("> applyManyToMany(Multi<").append(extractGenericType(inputType)).append("> input) {\n");
            source.append("        throw new RuntimeException(\"StepManyToMany not supported for local steps - implement specific logic if needed\");\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepSideEffect")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(returnType).append("> applySideEffect(").append(inputType).append(" input) {\n");
            source.append("        return service.process(input);\n");
            source.append("    }\n");
        }
        
        source.append("}\n");

        return source.toString();
    }

    private static void generateStepsRegistry(
            List<String> stepClassNames,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses
            ) {

        String pkg = "io.github.mbarcia.pipeline.generated";
        String simpleClassName = "StepsRegistryImpl";

        String sourceCode = generateStepsRegistrySourceCode(pkg, simpleClassName, stepClassNames);

        registerBuildItem(pkg, simpleClassName, sourceCode, generatedClasses, generatedResources, reflectiveClasses);
        
    }

    @BuildStep
    @io.quarkus.deployment.annotations.Record(ExecutionTime.STATIC_INIT)
    void registerStepsRegistry(
            StepsRegistryRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        String registryImplClassName = "io.github.mbarcia.pipeline.generated.StepsRegistryImpl";

        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(StepsRegistry.class)
                        .scope(ApplicationScoped.class)
                        .unremovable()
                        .runtimeValue(recorder.createStepsRegistry(registryImplClassName))
                        .defaultBean()
                        .done()
        );
    }

    private static String generateStepsRegistrySourceCode(
            String pkg,
            String simpleClassName,
            List<String> stepClassNames) {
        
        StringBuilder source = new StringBuilder();
        source.append("package ").append(pkg).append(";\n\n");
        
        // Add necessary imports - only basic ones, no step class imports
        // It is crucial not to try to import StepsRegistry as the compiler fails
        source.append("import ").append("java.util.List;\n");
        source.append("import ").append("java.util.ArrayList;\n\n");

        source.append("public class ").append(simpleClassName).append(" {\n\n");
        
        // Store only class names as strings to avoid indexing issues
        if (!stepClassNames.isEmpty()) {
            source.append("    // Only store class names\n");
            source.append("    private static final String[] STEP_CLASS_NAMES = new String[] {\n");
            for (int i = 0; i < stepClassNames.size(); i++) {
                String stepClassName = stepClassNames.get(i);
                source.append("        \"").append(stepClassName).append("\"");
                if (i < stepClassNames.size() - 1) {
                    source.append(",");
                }
                source.append("\n");
            }
            source.append("    };\n\n");
        } else {
            source.append("    private static final String[] STEP_CLASS_NAMES = new String[0];\n\n");
        }
        
        source.append("    public ").append(simpleClassName).append("() {\n");
        source.append("    }\n\n");
        
        source.append("    public List<Object> getSteps() {\n");
        source.append("        List<Object> stepsList = new ArrayList<>();\n");
        source.append("        for (String className : STEP_CLASS_NAMES) {\n");
        source.append("            try {\n");
        source.append("                Class<?> clazz = Class.forName(className);\n");
        source.append("                Object instance = clazz.getDeclaredConstructor().newInstance();\n");
        source.append("                stepsList.add(instance);\n");
        source.append("            } catch (Exception e) {\n");
        source.append("                throw new RuntimeException(\"Failed to instantiate step: \" + className, e);\n");
        source.append("            }\n");
        source.append("        }\n");
        source.append("        return stepsList;\n");
        source.append("    }\n");
        source.append("}\n");

        return source.toString();
    }

}