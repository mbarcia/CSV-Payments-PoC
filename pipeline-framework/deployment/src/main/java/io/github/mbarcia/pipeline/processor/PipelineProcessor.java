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

import io.github.mbarcia.pipeline.StepsRegistry;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.config.PipelineBuildTimeConfig;
import io.github.mbarcia.pipeline.recorder.StepsRegistryRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import jakarta.enterprise.context.ApplicationScoped;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

@SuppressWarnings("StringTemplateMigration")
public class PipelineProcessor {

    private static final String FEATURE_NAME = "pipeline-framework";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    void generateAdapters(CombinedIndexBuildItem combinedIndex,
                          PipelineBuildTimeConfig config,
                          BuildProducer<GeneratedClassBuildItem> generatedClasses,
                          BuildProducer<GeneratedResourceBuildItem> generatedResources,
                          BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        IndexView view = combinedIndex.getIndex();

        // Collect all step classes for application generation
        List<StepGeneratorMain.StepInfo> stepInfos = new ArrayList<>();

        for (AnnotationInstance ann : view.getAnnotations(DotName.createSimple(PipelineStep.class.getName()))) {
            ClassInfo stepClassInfo = ann.target().asClass();
            String stepType = ann.value("stepType") != null ? ann.value("stepType").asClass().name().toString() : "";
            // Extract order from annotation
            int order = ann.value("order") != null ? ann.value("order").asInt() : 0;

            // Store step info for application generation - using fully qualified class name
            String originalFqcn = stepClassInfo.name().toString();
            String originalPackage = originalFqcn.substring(0, originalFqcn.lastIndexOf('.'));
            String pkg = MessageFormat.format("{0}.pipeline", originalPackage);
            String simpleName = MessageFormat.format("{0}Step", stepClassInfo.simpleName());
            String generatedStepClassName = pkg + "." + simpleName;

            stepInfos.add(new StepGeneratorMain.StepInfo(
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
    }

    @BuildStep
    @io.quarkus.deployment.annotations.Record(io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT)
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

    // This is just for compilation. In the real implementation, the StepsRegistryImpl would be created
    // during the Maven generate-sources phase and the step classes would be available at runtime.
    private static void generateStepsRegistry(
            List<String> stepClassNames,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        String pkg = "io.github.mbarcia.pipeline.generated";
        String simpleClassName = "StepsRegistryImpl";

        String sourceCode = generateStepsRegistrySourceCode(pkg, simpleClassName, stepClassNames);

        registerBuildItem(pkg, simpleClassName, sourceCode, generatedClasses, generatedResources, reflectiveClasses);
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

    static String generateStepsRegistrySourceCode(
            String pkg,
            String simpleClassName,
            List<String> stepClassNames) {
        
        StringBuilder source = new StringBuilder();
        source.append("package ").append(pkg).append(";\n\n");
        
        // Add necessary imports - only basic ones, no step class imports
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