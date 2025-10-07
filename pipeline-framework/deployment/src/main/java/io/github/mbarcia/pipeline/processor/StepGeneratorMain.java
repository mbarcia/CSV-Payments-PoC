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
import io.quarkus.grpc.GrpcService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

/**
 * Main class to handle generation of pipeline step classes at runtime (via Maven execution).
 * This is a standalone class that can be invoked from Maven to generate step classes
 * during the generate-sources phase, avoiding issues with Quarkus build-time classloaders.
 */
public class StepGeneratorMain {

    // Helper class to store step information
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    static class StepInfo {
        final String stepClassName;
        final String stepType;
        final String stubName;
        final String grpcClientName;
        final int order;
        
        StepInfo(String stepClassName, String stepType, String stubName, String grpcClientName, int order) {
            this.stepClassName = stepClassName;
            this.stepType = stepType;
            this.stubName = stubName;
            this.grpcClientName = grpcClientName;
            this.order = order;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java StepGeneratorMain <output-dir> [generate-cli] [classpath-entries...]");
            System.exit(1);
        }

        String outputDir = args[0];
        boolean generateCli = false;
        List<String> classpathEntries = new ArrayList<>();
        
        if (args.length > 1) {
            // Second argument is generate-cli flag
            if ("true".equalsIgnoreCase(args[1]) || "false".equalsIgnoreCase(args[1])) {
                generateCli = Boolean.parseBoolean(args[1]);
                // Remaining arguments are classpath entries
                if (args.length > 2) {
                    classpathEntries.addAll(Arrays.asList(args).subList(2, args.length));
                }
            } else {
                // Backward compatibility: treat all args as classpath entries
                classpathEntries.addAll(Arrays.asList(args).subList(1, args.length));
            }
        }

        System.out.println("generate-cli=" + generateCli);

        try {
            generateSteps(outputDir, generateCli, classpathEntries);
        } catch (Exception e) {
            System.err.println("Error generating steps: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void generateSteps(String outputDir, boolean generateCli, List<String> classpathEntries) throws IOException {
        // Build index from the classpath entries
        IndexView index = buildIndex(classpathEntries);

        classpathEntries.forEach(p -> System.out.println("Classpath entry: " + p));
        index.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))
                .forEach(ai -> System.out.println("Found: " + ai.target().toString()));

        Path outputPath = Path.of(outputDir);
        Files.createDirectories(outputPath);

        // Debug output to see what we found
        System.out.println("StepGeneratorMain: Found " + index.getAnnotations(DotName.createSimple(PipelineStep.class.getName())).size() + " @PipelineStep annotations");
        
        // Collect all step classes for application generation
        List<StepInfo> stepInfos = new ArrayList<>();
        
        for (AnnotationInstance ann : index.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = ann.target().asClass();
            System.out.println("StepGeneratorMain: Processing class " + stepClassInfo.name());
            String stepType = ann.value("stepType") != null ? ann.value("stepType").asClass().name().toString() : "";
            boolean isLocal = ann.value("local") != null && ann.value("local").asBoolean();
            
            // Extract order from annotation
            int order = ann.value("order") != null ? ann.value("order").asInt() : 0;
            // Skip services with order < 1
            if (order < 1) continue;

            // extract annotation values with null checks (dollar sign stripped)
            String stubName = ann.value("grpcStub") != null ? ann.value("grpcStub").asClass().name().toString().replace('\u0024', '.') : "";
            String serviceName = stepClassInfo.name().toString();
            String inMapperName = ann.value("inboundMapper") != null ? ann.value("inboundMapper").asClass().name().toString() : "";
            String outMapperName = ann.value("outboundMapper") != null ? ann.value("outboundMapper").asClass().name().toString() : "";
            String grpcClientName = ann.value("grpcClient") != null ? ann.value("grpcClient").asString() : "";
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

            // Generate server-side impls for non-CLI builds
            // Note: For the Maven execution approach, we'll generate server-side classes always,
            // and client-side step classes only when generateCli is true
            boolean autoPersistenceEnabled = ann.value("autoPersist") == null || ann.value("autoPersist").asBoolean();

            if (!generateCli) {
                // Generate gRPC adapter class (server-side) - the service endpoint
                generateGrpcAdaptedServiceClass(stepClassInfo, backendType, inMapperName, outMapperName, serviceName, autoPersistenceEnabled, outputPath);
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
                    stubName,
                    grpcClientName,
                    order));
        }

        // Sort the stepInfos list by order before generating the StepsRegistry
        stepInfos.sort(Comparator.comparingInt(info -> info.order));

        // NOTE: StepsRegistryImpl is generated separately by the annotation processor
        // at build time to avoid classloader issues, so we're not generating it here.
        // See PipelineProcessor.java for the generation of StepsRegistryImpl
    }

    private static IndexView buildIndex(List<String> classpathEntries) throws IOException {
        Indexer indexer = new Indexer();
        
        System.out.println("StepGeneratorMain: Building index from classpath entries:");
        for (String classpathEntry : classpathEntries) {
            System.out.println("StepGeneratorMain: Processing classpath entry: " + classpathEntry);
        }
        
        // Add all classpath entries to the indexer
        for (String classpathEntry : classpathEntries) {
            Path path = Path.of(classpathEntry);
            if (Files.isDirectory(path)) {
                // Process directory
                System.out.println("StepGeneratorMain: Processing directory " + path);
                Files.walk(path)
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(classFile -> {
                        try {
                            indexer.index(Files.newInputStream(classFile));
                        } catch (IOException e) {
                            System.err.println("Could not index class: " + classFile + ", reason: " + e.getMessage());
                        }
                    });
            } else if (classpathEntry.endsWith(".jar")) {
                // Process JAR file
                System.out.println("StepGeneratorMain: Processing JAR file " + path);
                try (java.util.jar.JarInputStream jarStream = new java.util.jar.JarInputStream(Files.newInputStream(path))) {
                    java.util.jar.JarEntry entry;
                    while ((entry = jarStream.getNextJarEntry()) != null) {
                        if (entry.getName().endsWith(".class")) {
                            try {
                                // Read the class data into a byte array
                                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                                int nRead;
                                byte[] data = new byte[1024];
                                while ((nRead = jarStream.read(data, 0, data.length)) != -1) {
                                    buffer.write(data, 0, nRead);
                                }
                                buffer.flush();
                                byte[] classBytes = buffer.toByteArray();
                                
                                // Index the class bytes using ByteArrayInputStream
                                indexer.index(new java.io.ByteArrayInputStream(classBytes));
                                System.out.println("StepGeneratorMain: Indexed class from JAR: " + entry.getName());
                            } catch (Exception e) {
                                System.err.println("Could not index class from JAR: " + entry.getName() + ", reason: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        jarStream.closeEntry();
                    }
                } catch (Exception e) {
                    System.err.println("Could not process JAR file: " + path + ", reason: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        return indexer.complete();
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
        try {
            Path sourceFile = genDir.resolve(pkg.replace('.', '/') + "/" + simpleClassName + ".java");
            System.out.println("StepGeneratorMain: Writing source file to " + sourceFile);
            System.out.println("StepGeneratorMain: genDir = " + genDir);
            System.out.println("StepGeneratorMain: pkg = " + pkg);
            System.out.println("StepGeneratorMain: simpleClassName = " + simpleClassName);
            System.out.println("StepGeneratorMain: sourceFile.getParent() = " + sourceFile.getParent());
            Files.createDirectories(sourceFile.getParent());
            System.out.println("StepGeneratorMain: Created directories for " + sourceFile.getParent());
            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
            System.out.println("StepGeneratorMain: Wrote content to " + sourceFile);
            System.out.println(MessageFormat.format("Generated client class: {0}", sourceFile));
        } catch (IOException e) {
            System.err.println("Failed to write generated client source code file: " + e.getMessage());
            throw new RuntimeException("Failed to write generated client source code file", e);
        }
    }

    static String generateGrpcAdaptedServiceSourceCode(
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
                "import " + inMapperName + ";\n" +
                "import " + outMapperName + ";\n" +
                "import " + serviceName + ";\n" +
                "import " + "io.github.mbarcia.pipeline.persistence.PersistenceManager" + ";\n" +
                "import lombok.Getter;\n" +
                "import lombok.NoArgsConstructor;\n\n" +
                
                "@GrpcService\n" +
                "@ApplicationScoped\n" +
                "@NoArgsConstructor\n" +
                "@Getter\n" +
                "public class " + simpleClassName + " extends " + backendType + " {\n\n" +
                "    @Inject\n" +
                "    " + inMapperName + " inboundMapper;\n\n" +

                "    @Inject\n" +
                "    " + outMapperName + " outboundMapper;\n\n" +

                "    @Inject\n" +
                "    " + serviceName + " service;\n\n" +

                "    @Inject\n" +
                "    " + "io.github.mbarcia.pipeline.persistence.PersistenceManager" + " persistenceManager;\n\n" +

                "    @Override\n" +
                "    protected boolean isAutoPersistenceEnabled() {\n" +
                "        return " + autoPersistenceEnabled + ";\n" +
                "    }\n" +
                "}\n";
    }

}