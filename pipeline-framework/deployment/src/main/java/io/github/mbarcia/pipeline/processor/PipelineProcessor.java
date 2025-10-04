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
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.gizmo.*;
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

@SuppressWarnings("StringTemplateMigration")
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
                          BuildProducer<SyntheticBeanBuildItem> beans,
                          BuildProducer<UnremovableBeanBuildItem> unremovable,
                          BuildProducer<AdditionalBeanBuildItem> additionalBeans,
                          BuildSystemTargetBuildItem target) {

        IndexView view = combinedIndex.getIndex();
        
        Path genDir = target.getOutputDirectory().resolve("generated-sources/annotations");

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
            // Get output type from annotation
            String outputType = "";
            if (ann.value("outputType") != null) {
                outputType = ann.value("outputType").asClass().name().toString();
            }
            // Get gRPC input and output types from annotation
            String inputGrpcType = ann.value("inputGrpcType") != null ? ann.value("inputGrpcType").asClass().name().toString() : "";
            String outputGrpcType = ann.value("outputGrpcType") != null ? ann.value("outputGrpcType").asClass().name().toString() : "";
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
                generateGrpcClientStepClass(stepClassInfo, stubName, stepType, grpcClientValue, inputGrpcType, outputGrpcType, additionalBeans, genDir);
            }

            // Alternatively, do the "local-only" wrappers
            if (config.generateCli() && isLocal) {
                // Generate local step class (local service call) - the pipeline step
                generateLocalStepClass(
                        stepClassInfo,
                        stepType,
                        inputType,
                        outputType,  // Changed from outMapperName to outputType
                        additionalBeans,
                        genDir);
            }

            // Store step info for application generation - using fully qualified class name
            String originalFqcn = stepClassInfo.name().toString();
            String originalPackage = originalFqcn.substring(0, originalFqcn.lastIndexOf('.'));
            String pkg = MessageFormat.format("{0}.pipeline", originalPackage);
            String simpleName = MessageFormat.format("{0}Step", stepClassInfo.simpleName());
            String generatedStepClassName = pkg + "." + simpleName;

            stepInfos.add(new StepInfo(
                    generatedStepClassName,
                stepType
            ));
        }

        List<String> generatedStepClassNames = stepInfos.stream()
                .map(stepInfo -> stepInfo.stepClassName)
                .toList();

        if (config.generateCli() && !generatedStepClassNames.isEmpty()) {
            generateStepsRegistry(beans, unremovable, generatedStepClassNames, additionalBeans, genDir);
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

    private static void writeSourceFile(String pkg, String simpleClassName, String sourceCode, Path genDir) {
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
            BuildProducer<AdditionalBeanBuildItem> additionalBeans, Path genDir) {

        System.out.println("PipelineProcessor.generateGrpcClientStepClass: " + stepClassInfo.name());

        // Use the same package as the original service but with a ".pipeline" suffix
        String originalFqcn = stepClassInfo.name().toString();
        String originalPackage = originalFqcn.substring(0, originalFqcn.lastIndexOf('.'));
        String pkg = MessageFormat.format("{0}.pipeline", originalPackage);
        String simpleName = MessageFormat.format("{0}Step", stepClassInfo.simpleName());
        String fqcn = pkg + "." + simpleName;

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

        // Make the generated class an additional unremovable bean to ensure it's properly indexed
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(fqcn));
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

        // Use annotation values to get the explicit gRPC types
        // Since this method doesn't have access to the annotation values directly,
        // we need a different approach - for now, let me add the parameters to this method
        // First, I'll need to refactor to add the parameters and update the calling methods
        // For now, I'll revert to what was working - using the conversion method

        source.append("@ApplicationScoped\n");
        source.append("public class ").append(simpleName).append(" extends ConfigurableStep implements ").append(stepType.substring(stepType.lastIndexOf('.') + 1)).append("<").append(inputGrpcType).append(", ").append(outputGrpcType).append("> {\n\n");
        
        source.append("    @Inject\n");
        source.append("    @GrpcClient(\"").append(grpcClientValue).append("\")\n");
        source.append("    private ").append(stubName).append(" grpcClient;\n\n");
        
        source.append("    public ").append(simpleName).append("() {\n");
        source.append("    }\n\n");
        
        // Add the appropriate method implementation based on the step type
        if (stepType.endsWith("StepOneToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(outputGrpcType).append("> applyOneToOne(").append(inputGrpcType).append(" input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepOneToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(outputGrpcType).append("> applyOneToMany(").append(inputGrpcType).append(" input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToOne")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(outputGrpcType).append("> applyManyToOne(Multi<").append(inputGrpcType).append("> input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepManyToMany")) {
            source.append("    @Override\n");
            source.append("    public Multi<").append(outputGrpcType).append("> applyManyToMany(Multi<").append(inputGrpcType).append("> input) {\n");
            source.append("        return grpcClient.remoteProcess(input);\n");
            source.append("    }\n");
        } else if (stepType.endsWith("StepSideEffect")) {
            source.append("    @Override\n");
            source.append("    public Uni<").append(outputGrpcType).append("> applySideEffect(").append(inputGrpcType).append(" input) {\n");
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
    
    /**
     * Converts a domain type to its corresponding gRPC message type based on gRPC stub naming conventions.
     * This method attempts to infer the gRPC message type from the domain type and the gRPC stub class name.
     */
    private static String convertDomainTypeToGrpcType(String domainType, String stubName) {
        // Example: 
        // Domain: io.github.mbarcia.csv.common.domain.AckPaymentSent
        // Stub: io.github.mbarcia.csv.grpc.MutinyProcessAckPaymentSentServiceGrpc$ProcessAckPaymentSentServiceImplBase
        // Expected gRPC: io.github.mbarcia.csv.grpc.PaymentsProcessingSvc.AckPaymentSent

        // Extract the simple name of the domain type
        String domainSimpleName = domainType.substring(domainType.lastIndexOf('.') + 1);
        
        // Common package for gRPC messages
        String grpcPackage = "io.github.mbarcia.csv.grpc";
        
        // Determine the proto file class name
        String protoFileClassName = "PaymentsProcessingSvc"; // Default assumption based on examples
        
        // Try to determine the appropriate proto file class name based on the stub name
        if (stubName.contains("PaymentStatusSvcGrpc")) {
            protoFileClassName = "PaymentStatusSvc";
        } else if (stubName.contains("CsvFileProcessingSvcGrpc")) {
            if (stubName.contains("Input")) {
                protoFileClassName = "InputCsvFileProcessingSvc";
            } else {
                protoFileClassName = "OutputCsvFileProcessingSvc";
            }
        } else if (stubName.contains("OrchestratorGrpc")) {
            protoFileClassName = "Orchestrator";
        } else if (stubName.contains("PaymentsProcessingSvcGrpc")) {
            protoFileClassName = "PaymentsProcessingSvc";
        }
        
        // Return the gRPC message type
        return grpcPackage + "." + protoFileClassName + "." + domainSimpleName;
    }

    private static void generateLocalStepClass(
            ClassInfo serviceClassInfo,
            String stepType,
            String inputType,
            String outputType, BuildProducer<AdditionalBeanBuildItem> additionalBeans, Path genDir) {

        // Use the same package as the original service but with a ".pipeline" suffix
        String originalServicePackage = serviceClassInfo.name().toString();
        String basePackage = originalServicePackage.substring(0, originalServicePackage.lastIndexOf('.'));
        String pkg = basePackage + ".pipeline";
        String simpleName = serviceClassInfo.simpleName() + "Step";
        String stepClassName = pkg + "." + simpleName;

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

        // Make the generated class an additional unremovable bean to ensure it's properly indexed
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(stepClassName));
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

        source.append("@ApplicationScoped\n");
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
            BuildProducer<SyntheticBeanBuildItem> beans,
            BuildProducer<UnremovableBeanBuildItem> unremovable,
            List<String> stepClassNames, BuildProducer<AdditionalBeanBuildItem> additionalBeans, Path genDir) {
        
        // For the StepsRegistry, using the main pipeline package since it's a framework component
        String pkg = "io.github.mbarcia.pipeline.generated";
        String simpleClassName = "StepsRegistryImpl";

        // Generate the source code as a string
        String sourceCode = generateStepsRegistrySourceCode(
                pkg, 
                simpleClassName, 
                stepClassNames
        );

        writeSourceFile(pkg, simpleClassName, sourceCode, genDir);

        // Register the synthetic bean that provides the pre-computed steps list
        String registryImplClassName = pkg + "." + simpleClassName;
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
        
        // Add imports for all the step classes
        for (String stepClassName : stepClassNames) {
            source.append("import ").append(stepClassName).append(";\n");
        }
        source.append("\n");

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
            for (int i = 0; i < stepClassNames.size(); i++) {
                String stepClassName = stepClassNames.get(i);
                String stepSimpleClassName = stepClassName.substring(stepClassName.lastIndexOf('.') + 1);
                source.append("            Instance<").append(stepSimpleClassName).append("> stepProvider").append(i).append(" = instances.select(").append(stepSimpleClassName).append(".class);\n");
                source.append("            if (stepProvider").append(i).append(".isResolvable()) {\n");
                source.append("                stepsList.add(stepProvider").append(i).append(".get());\n");
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