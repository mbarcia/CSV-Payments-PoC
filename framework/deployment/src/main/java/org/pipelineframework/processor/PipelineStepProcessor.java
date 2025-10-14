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

package org.pipelineframework.processor;

import com.squareup.javapoet.*;
import io.quarkus.arc.Unremovable;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.pipelineframework.annotation.PipelineStep;
import org.pipelineframework.grpc.GrpcReactiveServiceAdapter;
import org.pipelineframework.step.StepOneToMany;
import org.pipelineframework.step.StepOneToOne;

/**
 * Java annotation processor that generates both gRPC client and server step implementations
 * based on @PipelineStep annotated service classes.
 */
@SupportedAnnotationTypes("org.pipelineframework.annotation.PipelineStep")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class PipelineStepProcessor extends AbstractProcessor {

    public static final String CLIENT_STEP_SUFFIX = "ClientStep";
    public static final String GRPC_SERVICE_SUFFIX = "GrpcService";
    private static final String PIPELINE_PACKAGE_SUFFIX = ".pipeline";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(PipelineStep.class)) {
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, 
                    "@PipelineStep can only be applied to classes", annotatedElement);
                continue;
            }

            TypeElement serviceClass = (TypeElement) annotatedElement;
            PipelineStep pipelineStep = serviceClass.getAnnotation(PipelineStep.class);

            try {
                generateGrpcServiceAdapter(serviceClass, pipelineStep);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate step (server) for " + serviceClass.getSimpleName() + ": " + e.getMessage());
            }

            try {
                generateClientStep(serviceClass, pipelineStep);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Failed to generate step (client) for " + serviceClass.getSimpleName() + ": " + e.getMessage());
            }
        }

        return true;
    }

    protected void generateClientStep(TypeElement serviceClass, PipelineStep pipelineStep) throws IOException {
        // Get the annotation mirror to extract TypeMirror values
        AnnotationMirror annotationMirror = getAnnotationMirror(serviceClass, PipelineStep.class);
        if (annotationMirror == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, 
                "Could not get annotation mirror for " + serviceClass, serviceClass);
            return;
        }

        // Extract values from the annotation mirror
        TypeMirror grpcStubType = getAnnotationValue(annotationMirror, "grpcStub");
        String grpcClientName = getAnnotationValueAsString(annotationMirror, "grpcClient");
        TypeMirror inboundMapperType = getAnnotationValue(annotationMirror, "inboundMapper");
        TypeMirror outboundMapperType = getAnnotationValue(annotationMirror, "outboundMapper");
        TypeMirror inputGrpcType = getAnnotationValue(annotationMirror, "inputGrpcType");
        TypeMirror outputGrpcType = getAnnotationValue(annotationMirror, "outputGrpcType");
        TypeMirror stepType = getAnnotationValue(annotationMirror, "stepType");

        // Create the package for the generated class
        // Use the same package as the original service but with a ".pipeline" suffix
        String packageName = processingEnv.getElementUtils()
            .getPackageOf(serviceClass).getQualifiedName().toString() + PIPELINE_PACKAGE_SUFFIX;
        
        // Create the simple name for the generated client step
        String serviceClassName = serviceClass.getSimpleName().toString();
        String clientStepClassName = serviceClassName.replace("Service", "") + CLIENT_STEP_SUFFIX;
        
        // Create the class with ApplicationScoped annotation for CDI
        TypeSpec.Builder clientStepBuilder = TypeSpec.classBuilder(clientStepClassName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.enterprise.context", "ApplicationScoped"))
                .build());

        // Add necessary imports as field declarations or annotations
        if (grpcStubType != null && !grpcStubType.toString().equals("void")) {
            // Add gRPC client field with @GrpcClient annotation
            FieldSpec grpcClientField = FieldSpec.builder(
                ClassName.get(grpcStubType),
                "grpcClient",
                Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(GrpcClient.class)
                    .addMember("value", "$S", grpcClientName)
                    .build())
                .build();
            
            clientStepBuilder.addField(grpcClientField);
        }
        
        // Add mapper fields with CDI injection if provided
        if (inboundMapperType != null && !inboundMapperType.toString().equals("void")) {
            FieldSpec inboundMapperField = FieldSpec.builder(
                ClassName.get(inboundMapperType),
                "inboundMapper",
                Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.inject", "Inject"))
                    .build())
                .build();
            clientStepBuilder.addField(inboundMapperField);
        }
        
        if (outboundMapperType != null && !outboundMapperType.toString().equals("void")) {
            FieldSpec outboundMapperField = FieldSpec.builder(
                ClassName.get(outboundMapperType),
                "outboundMapper",
                Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.inject", "Inject"))
                    .build())
                .build();
            clientStepBuilder.addField(outboundMapperField);
        }
        
        // Add default constructor
        MethodSpec constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .build();
        clientStepBuilder.addMethod(constructor);
        
        // Extend ConfigurableStep and implement the pipeline step interface based on stepType
        // Use gRPC types for the interface, as the client steps work with gRPC types directly
        // The server-side GrpcReactiveServiceAdapter handles gRPC-to-domain conversion
        ClassName configurableStep = ClassName.get("org.pipelineframework.step", "ConfigurableStep");
        clientStepBuilder.superclass(configurableStep);

        ClassName stepInterface;
        if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepOneToOne")) {
            stepInterface = ClassName.get(StepOneToOne.class);
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT));
        } else if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepOneToMany")) {
            stepInterface = ClassName.get(StepOneToMany.class);
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT));
        } else if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepManyToOne")) {
            // Handle StepManyToOne specifically
            stepInterface = ClassName.get("org.pipelineframework.step", "StepManyToOne");
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT));
        } else if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepManyToMany")) {
            // Handle StepManyToMany
            stepInterface = ClassName.get("org.pipelineframework.step", "StepManyToMany");
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT));
        } else {
            // Default to OneToOne for simplicity; can extend to support more types
            stepInterface = ClassName.get(StepOneToOne.class);
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT));
        }
        
        // Add the apply method implementation based on the step type
        // No mapping needed - client steps work directly with gRPC types
        // Server-side GrpcReactiveServiceAdapter handles gRPC-to-domain conversion
        if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepOneToMany")) {
            // For OneToMany: Input -> Multi<Output> (StepOneToMany interface has applyOneToMany(Input in) method)
            MethodSpec applyMethod = MethodSpec.methodBuilder("applyOneToMany")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Multi.class), 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT))
                .addParameter(inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, "input")
                .addStatement("return this.grpcClient.remoteProcess(input)")
                .build();
                
            clientStepBuilder.addMethod(applyMethod);
        } else if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepManyToOne")) {
            // For ManyToOne: Multi<Input> -> Uni<Output> (ManyToOne interface has applyBatchMulti(Multi<Input> in) method)
            MethodSpec applyMethod = MethodSpec.methodBuilder("applyBatchMulti")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Uni.class), 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Multi.class), 
                    inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT), "inputs")
                .addStatement("return this.grpcClient.remoteProcess(inputs)")
                .build();
                
            clientStepBuilder.addMethod(applyMethod);
        } else {
            // Default to OneToOne: Input -> Uni<Output> (StepOneToOne interface has applyOneToOne(Input in) method)
            MethodSpec applyMethod = MethodSpec.methodBuilder("applyOneToOne")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Uni.class), 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT))
                .addParameter(inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, "input")
                .addStatement("return this.grpcClient.remoteProcess(input)")
                .build();
                
            clientStepBuilder.addMethod(applyMethod);
        }
        
        TypeSpec clientStepClass = clientStepBuilder.build();
        
        // Write the generated class
        JavaFile javaFile = JavaFile.builder(packageName, clientStepClass)
            .build();
            
        JavaFileObject builderFile = processingEnv.getFiler()
            .createSourceFile(packageName + "." + clientStepClassName);
        
        try (var writer = builderFile.openWriter()) {
            javaFile.writeTo(writer);
        }
    }

    /**
     * Generates and writes a gRPC service adapter class for the given service annotated with @PipelineStep.
     *
     * The generated class is placed in the original service package with a ".pipeline" suffix, extends an
     * appropriate gRPC base implementation, is annotated for CDI and gRPC wiring, injects the original service
     * and optional mappers/persistence manager, and exposes a remoteProcess method implemented via an inline
     * adapter chosen according to the configured step type.
     *
     * @param serviceClass the TypeElement representing the annotated service class
     * @param pipelineStep the PipelineStep annotation instance for the service (used to read configured values)
     * @throws IOException if writing the generated Java source file fails
     */
    protected void generateGrpcServiceAdapter(TypeElement serviceClass, PipelineStep pipelineStep) throws IOException {
        // Get the annotation mirror to extract TypeMirror values
        AnnotationMirror annotationMirror = getAnnotationMirror(serviceClass, PipelineStep.class);
        if (annotationMirror == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, 
                "Could not get annotation mirror for " + serviceClass, serviceClass);
            return;
        }

        // Extract values from the annotation mirror
        TypeMirror inputType = getAnnotationValue(annotationMirror, "inputType");
        TypeMirror outputType = getAnnotationValue(annotationMirror, "outputType");
        TypeMirror inboundMapperType = getAnnotationValue(annotationMirror, "inboundMapper");
        TypeMirror outboundMapperType = getAnnotationValue(annotationMirror, "outboundMapper");
        TypeMirror inputGrpcType = getAnnotationValue(annotationMirror, "inputGrpcType");
        TypeMirror outputGrpcType = getAnnotationValue(annotationMirror, "outputGrpcType");
        TypeMirror stepType = getAnnotationValue(annotationMirror, "stepType");
        TypeMirror grpcImplType = getAnnotationValue(annotationMirror, "grpcImpl");
        String serviceName = serviceClass.getQualifiedName().toString();
        TypeMirror backendType = getAnnotationValue(annotationMirror, "backendType");
        boolean autoPersistenceEnabled = getAnnotationValueAsBoolean(annotationMirror, "autoPersist", true);

        // Use the same package as the original service but with a ".pipeline" suffix
        String fqcn = serviceClass.getQualifiedName().toString();
        String originalPackage = fqcn.substring(0, fqcn.lastIndexOf('.'));
        String pkg = String.format("%s%s", originalPackage, PIPELINE_PACKAGE_SUFFIX);
        String simpleClassName = String.format("%s%s", serviceClass.getSimpleName().toString(), GRPC_SERVICE_SUFFIX);

        // Determine the backend type (default to GrpcReactiveServiceAdapter)
        ClassName backendClassName;
        if (backendType != null) {
            // Convert TypeMirror to ClassName
            String backendTypeStr = backendType.toString();
            // Split the name to package and simple name
            int lastDot = backendTypeStr.lastIndexOf('.');
            if (lastDot > 0) {
                String packageName = backendTypeStr.substring(0, lastDot);
                String simpleName = backendTypeStr.substring(lastDot + 1);
                backendClassName = ClassName.get(packageName, simpleName);
            } else {
                // If no package, just use the simple name
                backendClassName = ClassName.get("", backendTypeStr);
            }
        } else {
            backendClassName = ClassName.get(GrpcReactiveServiceAdapter.class);
        }

        // Determine the appropriate gRPC service base class based on the grpcImpl annotation value
        ClassName grpcBaseClassName;
        if (grpcImplType != null && !grpcImplType.toString().equals("void")) {
            // Use the grpcImpl class as the base class
            String grpcImplTypeStr = grpcImplType.toString();
            int lastDot = grpcImplTypeStr.lastIndexOf('.');
            if (lastDot > 0) {
                String packageName = grpcImplTypeStr.substring(0, lastDot);
                String simpleName = grpcImplTypeStr.substring(lastDot + 1);
                grpcBaseClassName = ClassName.get(packageName, simpleName);
            } else {
                grpcBaseClassName = ClassName.get("", grpcImplTypeStr);
            }
        } else {
            // Fallback to determine the package from available gRPC types
            // Default to the original package with .grpc suffix if no gRPC types available
            String grpcPackage = originalPackage + ".grpc";
            
            // Try to determine the actual gRPC package from inputGrpcType or outputGrpcType
            if (inputGrpcType != null && !inputGrpcType.toString().equals("void")) {
                grpcPackage = extractPackage(inputGrpcType, grpcPackage);
            } else if (outputGrpcType != null && !outputGrpcType.toString().equals("void")) {
                grpcPackage = extractPackage(outputGrpcType, grpcPackage);
            }
            
            // Construct the gRPC service base class using the determined package
            String grpcServiceBaseClass = grpcPackage + "." + 
                serviceClass.getSimpleName().toString().replace("Service", "") + "Grpc." + 
                serviceClass.getSimpleName().toString().replace("Service", "") + "ImplBase";
            int lastDot = grpcServiceBaseClass.lastIndexOf('.');
            if (lastDot > 0) {
                String packageName = grpcServiceBaseClass.substring(0, lastDot);
                String simpleName = grpcServiceBaseClass.substring(lastDot + 1);
                grpcBaseClassName = ClassName.get(packageName, simpleName);
            } else {
                grpcBaseClassName = ClassName.get("", grpcServiceBaseClass);
            }
        }

        // Create the gRPC service class
        TypeSpec.Builder grpcServiceBuilder = TypeSpec.classBuilder(simpleClassName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(ClassName.get(GrpcService.class)).build())
            .addAnnotation(AnnotationSpec.builder(ClassName.get("jakarta.inject", "Singleton")).build())
            .addAnnotation(AnnotationSpec.builder(Unremovable.class).build())
            .superclass(grpcBaseClassName); // Extend the actual gRPC service base class

        // Add field declarations
        if (inboundMapperType != null && !inboundMapperType.toString().equals("void")) {
            FieldSpec inboundMapperField = FieldSpec.builder(
                ClassName.get(inboundMapperType),
                "inboundMapper")
                .addAnnotation(AnnotationSpec.builder(Inject.class).build())
                .build();
            grpcServiceBuilder.addField(inboundMapperField);
        }

        if (outboundMapperType != null && !outboundMapperType.toString().equals("void")) {
            FieldSpec outboundMapperField = FieldSpec.builder(
                ClassName.get(outboundMapperType),
                "outboundMapper")
                .addAnnotation(AnnotationSpec.builder(Inject.class).build())
                .build();
            grpcServiceBuilder.addField(outboundMapperField);
        }

        FieldSpec serviceField = FieldSpec.builder(
            ClassName.get(serviceClass),
            "service")
            .addAnnotation(AnnotationSpec.builder(Inject.class).build())
            .build();
        grpcServiceBuilder.addField(serviceField);

        // Add persistence manager field
        FieldSpec persistenceManagerField = FieldSpec.builder(
                ClassName.get("org.pipelineframework.persistence", "PersistenceManager"),
                "persistenceManager")
            .addAnnotation(AnnotationSpec.builder(Inject.class).build())
            .build();
        grpcServiceBuilder.addField(persistenceManagerField);

        // Determine which gRPC adapter to use based on the step type
        ClassName grpcAdapterClassName;
        if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepOneToMany")) {
            // For OneToMany: unary input -> streaming output
            grpcAdapterClassName = ClassName.get("org.pipelineframework.grpc", "GrpcServiceStreamingAdapter");
        } else if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepManyToOne")) {
            // For ManyToOne: streaming input -> unary output
            grpcAdapterClassName = ClassName.get("org.pipelineframework.grpc", "GrpcServiceClientStreamingAdapter");
        } else {
            // Default to GrpcReactiveServiceAdapter for OneToOne, ManyToMany, etc.
            grpcAdapterClassName = ClassName.get("org.pipelineframework.grpc", "GrpcReactiveServiceAdapter");
        }

        // Add the required gRPC service method implementation based on the gRPC service base class
        // For Mutiny gRPC services, all methods return Uni/Multi, not use StreamObserver
        // Following the manual template, the adapter is created inline inside the method
        if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepOneToMany")) {
            // For server streaming (unary input, streaming output) - e.g., ProcessFolderService
            // Creates the adapter inline as an anonymous class inside the method
            TypeSpec inlineAdapter = TypeSpec.anonymousClassBuilder("")
                .superclass(ParameterizedTypeName.get(grpcAdapterClassName, 
                    inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT, 
                    inputType != null ? ClassName.get(inputType) : ClassName.OBJECT, 
                    outputType != null ? ClassName.get(outputType) : ClassName.OBJECT))
                .addMethod(MethodSpec.methodBuilder("getService")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(ClassName.get(serviceClass))
                    .addStatement("return $N", "service")
                    .build())
                .addMethod(MethodSpec.methodBuilder("fromGrpc")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(inputType != null ? ClassName.get(inputType) : ClassName.OBJECT)
                    .addParameter(inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, "grpcIn")
                    .addStatement("return $N.fromGrpcFromDto(grpcIn)", "inboundMapper")
                    .build())
                .addMethod(MethodSpec.methodBuilder("toGrpc")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT)
                    .addParameter(outputType != null ? ClassName.get(outputType) : ClassName.OBJECT, "output")
                    .addStatement("return $N.toDtoToGrpc(output)", "outboundMapper")
                    .build())
                .addMethod(MethodSpec.methodBuilder("getStepConfig")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(ClassName.get("org.pipelineframework.config", "StepConfig"))
                    .addStatement("return new org.pipelineframework.config.StepConfig().autoPersist($L)", autoPersistenceEnabled)
                    .build())
                .build();

            MethodSpec remoteProcessMethod = MethodSpec.methodBuilder("remoteProcess")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get("io.smallrye.mutiny", "Multi"), 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT))
                .addParameter(inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, "request")
                .addStatement("$T adapter = $L", 
                    ParameterizedTypeName.get(grpcAdapterClassName, 
                        inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                        outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT, 
                        inputType != null ? ClassName.get(inputType) : ClassName.OBJECT, 
                        outputType != null ? ClassName.get(outputType) : ClassName.OBJECT), 
                    inlineAdapter)
                .addStatement("adapter.setPersistenceManager(this.persistenceManager)")
                .addStatement("return adapter.remoteProcess(request)")
                .build();
            grpcServiceBuilder.addMethod(remoteProcessMethod);
        } else if (stepType != null && stepType.toString().equals("org.pipelineframework.step.StepManyToOne")) {
            // For client streaming (streaming input, unary output) - e.g., ProcessCsvPaymentsOutputFileService
            TypeSpec inlineAdapter = TypeSpec.anonymousClassBuilder("")
                .superclass(ParameterizedTypeName.get(grpcAdapterClassName, 
                    inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT, 
                    inputType != null ? ClassName.get(inputType) : ClassName.OBJECT, 
                    outputType != null ? ClassName.get(outputType) : ClassName.OBJECT))
                .addMethod(MethodSpec.methodBuilder("getService")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(ClassName.get(serviceClass))
                    .addStatement("return $N", "service")
                    .build())
                .addMethod(MethodSpec.methodBuilder("fromGrpc")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(inputType != null ? ClassName.get(inputType) : ClassName.OBJECT)
                    .addParameter(inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, "grpcIn")
                    .addStatement("return $N.fromGrpcFromDto(grpcIn)", "inboundMapper")
                    .build())
                .addMethod(MethodSpec.methodBuilder("toGrpc")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT)
                    .addParameter(outputType != null ? ClassName.get(outputType) : ClassName.OBJECT, "output")
                    .addStatement("return $N.toDtoToGrpc(output)", "outboundMapper")
                    .build())
                .addMethod(MethodSpec.methodBuilder("getStepConfig")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(ClassName.get("org.pipelineframework.config", "StepConfig"))
                    .addStatement("return new org.pipelineframework.config.StepConfig().autoPersist($L)", autoPersistenceEnabled)
                    .build())
                .build();

            MethodSpec remoteProcessMethod = MethodSpec.methodBuilder("remoteProcess")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get("io.smallrye.mutiny", "Uni"), 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT))
                .addParameter(ParameterizedTypeName.get(ClassName.get("io.smallrye.mutiny", "Multi"), 
                    inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT), "request")
                .addStatement("$T adapter = $L", 
                    ParameterizedTypeName.get(grpcAdapterClassName, 
                        inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                        outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT, 
                        inputType != null ? ClassName.get(inputType) : ClassName.OBJECT, 
                        outputType != null ? ClassName.get(outputType) : ClassName.OBJECT), 
                    inlineAdapter)
                .addStatement("adapter.setPersistenceManager(this.persistenceManager)")
                .addStatement("return adapter.remoteProcess(request)")
                .build();
            grpcServiceBuilder.addMethod(remoteProcessMethod);
        } else {
            // Default to unary (unary input, unary output) - e.g., ProcessPaymentStatusGrpcService
            TypeSpec inlineAdapter = TypeSpec.anonymousClassBuilder("")
                .superclass(ParameterizedTypeName.get(grpcAdapterClassName, 
                    inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT, 
                    inputType != null ? ClassName.get(inputType) : ClassName.OBJECT, 
                    outputType != null ? ClassName.get(outputType) : ClassName.OBJECT))
                .addMethod(MethodSpec.methodBuilder("getService")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(ClassName.get(serviceClass))
                    .addStatement("return $N", "service")
                    .build())
                .addMethod(MethodSpec.methodBuilder("fromGrpc")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(inputType != null ? ClassName.get(inputType) : ClassName.OBJECT)
                    .addParameter(inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, "grpcIn")
                    .addStatement("return $N.fromGrpcFromDto(grpcIn)", "inboundMapper")
                    .build())
                .addMethod(MethodSpec.methodBuilder("toGrpc")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT)
                    .addParameter(outputType != null ? ClassName.get(outputType) : ClassName.OBJECT, "output")
                    .addStatement("return $N.toDtoToGrpc(output)", "outboundMapper")
                    .build())
                .addMethod(MethodSpec.methodBuilder("getStepConfig")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PROTECTED)
                    .returns(ClassName.get("org.pipelineframework.config", "StepConfig"))
                    .addStatement("return new org.pipelineframework.config.StepConfig().autoPersist($L)", autoPersistenceEnabled)
                    .build())
                .build();

            MethodSpec remoteProcessMethod = MethodSpec.methodBuilder("remoteProcess")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get("io.smallrye.mutiny", "Uni"), 
                    outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT))
                .addParameter(inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, "request")
                .addStatement("$T adapter = $L", 
                    ParameterizedTypeName.get(grpcAdapterClassName, 
                        inputGrpcType != null ? ClassName.get(inputGrpcType) : ClassName.OBJECT, 
                        outputGrpcType != null ? ClassName.get(outputGrpcType) : ClassName.OBJECT, 
                        inputType != null ? ClassName.get(inputType) : ClassName.OBJECT, 
                        outputType != null ? ClassName.get(outputType) : ClassName.OBJECT), 
                    inlineAdapter)
                .addStatement("adapter.setPersistenceManager(this.persistenceManager)")
                .addStatement("return adapter.remoteProcess(request)")
                .build();
            grpcServiceBuilder.addMethod(remoteProcessMethod);
        }

        TypeSpec grpcServiceClass = grpcServiceBuilder.build();

        // Write the generated class
        JavaFile javaFile = JavaFile.builder(pkg, grpcServiceClass)
            .build();

        JavaFileObject builderFile = processingEnv.getFiler()
            .createSourceFile(pkg + "." + simpleClassName);

        try (var writer = builderFile.openWriter()) {
            javaFile.writeTo(writer);
        }
    }

    private static String extractPackage(TypeMirror inputGrpcType, String grpcPackage) {
        String inputGrpcTypeStr = inputGrpcType.toString();
        int lastDot = inputGrpcTypeStr.lastIndexOf('.');
        if (lastDot > 0) {
            grpcPackage = inputGrpcTypeStr.substring(0, lastDot);
        }
        return grpcPackage;
    }

    protected AnnotationMirror getAnnotationMirror(Element element, Class<?> annotationClass) {
        String annotationClassName = annotationClass.getCanonicalName();
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotationClassName)) {
                return annotationMirror;
            }
        }
        return null;
    }

    protected TypeMirror getAnnotationValue(AnnotationMirror annotation, String memberName) {
        for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
            if (executableElement.getSimpleName().toString().equals(memberName)) {
                AnnotationValue annotationValue = annotation.getElementValues().get(executableElement);
                if (annotationValue != null) {
                    Object value = annotationValue.getValue();
                    if (value instanceof TypeMirror) {
                        return (TypeMirror) value;
                    } else if (value instanceof String) {
                        // Handle string values that should be class names
                        String className = (String) value;
                        if ("void".equals(className) || "".equals(className) || "java.lang.Void".equals(className)) {
                            // Return null for void types
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected String getAnnotationValueAsString(AnnotationMirror annotation, String memberName) {
        for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
            if (executableElement.getSimpleName().toString().equals(memberName)) {
                Object value = annotation.getElementValues().get(executableElement).getValue();
                if (value instanceof String) {
                    return (String) value;
                } else if (value instanceof TypeMirror) {
                    // For class types, get the qualified name
                    return ((TypeMirror) value).toString();
                }
            }
        }
        return null;
    }

    protected boolean getAnnotationValueAsBoolean(AnnotationMirror annotation, String memberName, boolean defaultValue) {
        for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
            if (executableElement.getSimpleName().toString().equals(memberName)) {
                Object value = annotation.getElementValues().get(executableElement).getValue();
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
                break; // Exit after finding the element even if it's not the correct type
            }
        }
        return defaultValue;
    }
}