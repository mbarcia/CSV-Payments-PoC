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
import java.lang.reflect.Modifier;
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

        ClassOutput classOutput = (name, data) -> {
            generatedClasses.produce(new GeneratedClassBuildItem(true, name, data));

            Path out = Paths.get("target/generated-debug", name.replace('.', '/') + ".class");

            try {
                Files.createDirectories(out.getParent());
                Files.write(out, data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        try (ClassCreator cc = new ClassCreator(classOutput, adapterClassName, null, backendType)) {
            // Add @GrpcService and a CDI scope
            cc.addAnnotation(GrpcService.class);
            cc.addAnnotation(ApplicationScoped.class);

            // Add @Inject fields
            cc.getFieldCreator("inboundMapper", inMapperName)
                    .setModifiers(Modifier.PRIVATE)
                    .addAnnotation(Inject.class);

            cc.getFieldCreator("outboundMapper", outMapperName)
                    .setModifiers(Modifier.PRIVATE)
                    .addAnnotation(Inject.class);

            cc.getFieldCreator("service", serviceName)
                    .setModifiers(Modifier.PRIVATE)
                    .addAnnotation(Inject.class);

            cc.getFieldCreator("persistenceManager", PersistenceManager.class.getName())
                    .setModifiers(Modifier.PRIVATE)
                    .addAnnotation(Inject.class);

            // Add default no-arg constructor that calls super()
            try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                defaultCtor.invokeSpecialMethod(
                        MethodDescriptor.ofConstructor(backendType),
                        defaultCtor.getThis()
                );
                defaultCtor.returnValue(null);
            }
        }

        System.out.println(MessageFormat.format("Generated gRPC service adapter: {0}", adapterClassName));
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

        // Generate the client step class as bytecode (since it needs to be available for indexing)
        String pkg = "io.github.mbarcia.pipeline.generated";
        String simpleName = stepClassInfo.simpleName() + "Step";
        String fqcn = pkg + "." + simpleName;

        ClassOutput classOutput = (name, data) -> {
            generatedClasses.produce(new GeneratedClassBuildItem(true, name, data));
        };
        
        try (ClassCreator cc = new ClassCreator(
                classOutput,
                fqcn, 
                null, 
                Object.class.getName(), 
                stepType)) {

            // Add @ApplicationScoped annotation
            cc.addAnnotation(ApplicationScoped.class);

            // Create field with @Inject and @GrpcClient annotations
            FieldCreator fieldCreator = cc.getFieldCreator("grpcClient", stubName)
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            fieldCreator.addAnnotation(Inject.class);
            
            // Add @GrpcClient annotation using Gizmo's approach
            AnnotationCreator grpcClientAnnotation = fieldCreator.addAnnotation(GrpcClient.class);
            grpcClientAnnotation.addValue("value", grpcClientValue);

            // Add default no-arg constructor
            try (MethodCreator defaultCtor = cc.getMethodCreator("<init>", void.class)) {
                defaultCtor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class.getName()), defaultCtor.getThis());
                defaultCtor.returnValue(null);
            }

            // Add the appropriate method implementation based on the step type
            if (stepType.endsWith("StepOneToOne")) {
                try (MethodCreator method = cc.getMethodCreator("applyOneToOne", UNI, inputType)) {
                    ResultHandle inputParam = method.getMethodParam(0);
                    ResultHandle client = method.readInstanceField(FieldDescriptor.of(fqcn, "grpcClient", stubName), method.getThis());
                    ResultHandle result = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(stubName, "remoteProcess", UNI, inputType),
                            client, inputParam);
                    method.returnValue(result);
                }
            } else if (stepType.endsWith("StepOneToMany")) {
                try (MethodCreator method = cc.getMethodCreator("applyOneToMany", MULTI, inputType)) {
                    ResultHandle inputParam = method.getMethodParam(0);
                    ResultHandle client = method.readInstanceField(FieldDescriptor.of(fqcn, "grpcClient", stubName), method.getThis());
                    ResultHandle result = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(stubName, "remoteProcess", MULTI, inputType),
                            client, inputParam);
                    method.returnValue(result);
                }
            } else if (stepType.endsWith("StepManyToOne")) {
                try (MethodCreator method = cc.getMethodCreator("applyManyToOne", UNI, MULTI)) {
                    ResultHandle inputParam = method.getMethodParam(0);
                    ResultHandle client = method.readInstanceField(FieldDescriptor.of(fqcn, "grpcClient", stubName), method.getThis());
                    ResultHandle result = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(stubName, "remoteProcess", UNI, MULTI),
                            client, inputParam);
                    method.returnValue(result);
                }
            } else if (stepType.endsWith("StepManyToMany")) {
                try (MethodCreator method = cc.getMethodCreator("applyManyToMany", MULTI, MULTI)) {
                    ResultHandle inputParam = method.getMethodParam(0);
                    ResultHandle client = method.readInstanceField(FieldDescriptor.of(fqcn, "grpcClient", stubName), method.getThis());
                    ResultHandle result = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(stubName, "remoteProcess", MULTI, MULTI),
                            client, inputParam);
                    method.returnValue(result);
                }
            } else if (stepType.endsWith("StepSideEffect")) {
                try (MethodCreator method = cc.getMethodCreator("applySideEffect", UNI, inputType)) {
                    ResultHandle inputParam = method.getMethodParam(0);
                    ResultHandle client = method.readInstanceField(FieldDescriptor.of(fqcn, "grpcClient", stubName), method.getThis());
                    ResultHandle result = method.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(stubName, "remoteProcess", UNI, inputType),
                            client, inputParam);
                    method.returnValue(result);
                }
            }

            // Make the generated class an additional unremovable bean to ensure it's properly indexed
            additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(fqcn));
        }
    }

    private static void generateLocalStepClass(
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            ClassInfo serviceClassInfo,
            String stepType,
            String inputType,
            String outputMapperName, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Generate a subclass that implements the appropriate step interface
        // Use the same generated package as gRPC client steps to avoid conflicts and match registry expectations
        String pkg = "io.github.mbarcia.pipeline.generated";
        String simpleName = serviceClassInfo.simpleName() + "Step";
        String stepClassName = pkg + "." + simpleName;

        ClassOutput classOutput = (name, data) -> {
            generatedClasses.produce(new GeneratedClassBuildItem(true, name, data));
        };

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
        }
        System.out.println("PipelineProcessor.generateLocalStepClass: " + stepClassName);
        
        // Make the generated class an additional unremovable bean to ensure it's properly indexed
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(stepClassName));
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
            MethodDescriptor.ofMethod("io.smallrye.mutiny.Multi", "createFrom", "io.smallrye.mutiny.Multi", "java.util.List"),
            domainList
        );

        // Return the Multi - this implements StepOneToMany.applyOneToMany correctly
        method.returnValue(multiFromList);
    }

    private static void generateLocalStepOneToOne(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        // For StepOneToOne<I, O>, call the service and return the result
        ResultHandle inputParam = method.getMethodParam(0);
        ResultHandle service = method.readInstanceField(serviceField.getFieldDescriptor(), method.getThis());

        // Call service.process(input) - assume the service implements ReactiveService with the right signature
        // Use reflection to find the actual method name and signature from the service class
        ResultHandle serviceResult = method.invokeVirtualMethod(
            MethodDescriptor.ofMethod(serviceClassInfo.name().toString(), "process", UNI, "java.lang.Object"),
            service, inputParam
        );

        method.returnValue(serviceResult);
    }

    private static void generateLocalStepManyToOne(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        // For StepManyToOne<Multi<I>, O>, collect the Multi, process and return Uni<O>
        ResultHandle inputParam = method.getMethodParam(0);

        // For a local StepManyToOne, we'd need to have a service that can handle collections
        // Since this is complex and may not be the default pattern, throw an exception for now
        // to indicate this step type is not supported for local steps
        method.throwException(RuntimeException.class, "StepManyToOne not supported for local steps - implement specific logic if needed");
    }

    private static void generateLocalStepManyToMany(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        // For StepManyToMany<Multi<I>, Multi<O>>, map each item in the Multi to another Multi
        ResultHandle inputParam = method.getMethodParam(0);
        
        // A common pattern would be to flatMap each input item to a Multi output
        // For example: input Multi<String> -> service processes each and returns Multi<Output>
        
        // Create a lambda function that processes each item
        // This would require more complex Gizmo code to create a lambda/functional interface
        method.throwException(RuntimeException.class, "StepManyToMany not supported for local steps - implement specific logic if needed");
    }

    private static void generateLocalStepSideEffect(MethodCreator method, FieldCreator serviceField, FieldCreator mapperField, ClassInfo serviceClassInfo, String outputMapperName) {
        // For StepSideEffect<I>, call the service and return Uni<Void>
        ResultHandle inputParam = method.getMethodParam(0);
        ResultHandle service = method.readInstanceField(serviceField.getFieldDescriptor(), method.getThis());

        // Call service.process(input) 
        ResultHandle serviceResult = method.invokeVirtualMethod(
            MethodDescriptor.ofMethod(serviceClassInfo.name().toString(), "process", UNI, "java.lang.Object"),
            service, inputParam
        );

        method.returnValue(serviceResult);
    }
    
    private static void generateStepsRegistry(
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<SyntheticBeanBuildItem> beans,
            BuildProducer<UnremovableBeanBuildItem> unremovable,
            List<String> stepClassNames, BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        
        // Generate an implementation of StepsRegistry that returns the registered steps as bytecode using Gizmo
        String registryImplClassName = "io.github.mbarcia.pipeline.generated.StepsRegistryImpl";

        ClassOutput classOutput = (name, data) -> {
            generatedClasses.produce(new GeneratedClassBuildItem(true, name, data));
        };
        
        try (ClassCreator cc = ClassCreator.builder()
                .classOutput(classOutput)
                .className(registryImplClassName)
                .interfaces("io.github.mbarcia.pipeline.StepsRegistry")
                .build()) {

            // Add @ApplicationScoped annotation
            cc.addAnnotation(ApplicationScoped.class);

            // Add field for CDI instances
            FieldCreator instancesField = cc.getFieldCreator("instances", "jakarta.enterprise.inject.Instance")
                    .setModifiers(java.lang.reflect.Modifier.PRIVATE);
            instancesField.addAnnotation(Inject.class);

            // Add default no-arg constructor
            try (MethodCreator ctor = cc.getMethodCreator("<init>", void.class)) {
                ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
                ctor.returnValue(null);
            }

            // Implement the getSteps method
            try (MethodCreator getStepsMethod = cc.getMethodCreator("getSteps", "java.util.List")) {
                // Create the list to return
                ResultHandle stepsList = getStepsMethod.newInstance(
                        MethodDescriptor.ofConstructor("java.util.ArrayList"),
                        new ResultHandle[0]
                );

                if (!stepClassNames.isEmpty()) {
                    // Check if instances is not null before proceeding
                    ResultHandle instances = getStepsMethod.readInstanceField(
                            instancesField.getFieldDescriptor(), 
                            getStepsMethod.getThis()
                    );
                    
                    // Create conditional block to check if instances is not null
                    BytecodeCreator ifInstancesNotNull = getStepsMethod.ifNotNull(instances).trueBranch();
                    
                    // Loop through each step class name and add to list
                    for (String stepClassName : stepClassNames) {
                        // Create Instance<StepClass> stepProvider = instances.select(StepClass.class);
                        ResultHandle stepClass = ifInstancesNotNull.loadClass(stepClassName);
                        ResultHandle stepProvider = ifInstancesNotNull.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(
                                        "jakarta.enterprise.inject.Instance", 
                                        "select", 
                                        "jakarta.enterprise.inject.Instance", 
                                        "java.lang.Class"
                                ),
                                instances, 
                                stepClass
                        );
                        
                        // Check if the step provider is resolvable
                        ResultHandle isResolvable = ifInstancesNotNull.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(
                                        "jakarta.enterprise.inject.Instance", 
                                        "isResolvable", 
                                        boolean.class
                                ),
                                stepProvider
                        );
                        
                        // Create conditional block for resolvable check
                        BytecodeCreator ifResolvable = ifInstancesNotNull.ifNonZero(isResolvable).trueBranch();
                        
                        // Get the step instance and add to list
                        ResultHandle stepInstance = ifResolvable.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(
                                        "jakarta.enterprise.inject.Instance", 
                                        "get", 
                                        Object.class
                                ),
                                stepProvider
                        );
                        
                        ifResolvable.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(
                                        "java.util.List", 
                                        "add", 
                                        boolean.class, 
                                        Object.class
                                ),
                                stepsList, 
                                stepInstance
                        );
                    }
                }

                getStepsMethod.returnValue(stepsList);
            }
        }

        System.out.println("Successfully generated StepsRegistryImpl class");

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

}