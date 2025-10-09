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

import com.squareup.javapoet.*;
import io.github.mbarcia.pipeline.GenericGrpcReactiveServiceAdapter;
import io.github.mbarcia.pipeline.annotation.PipelineStep;
import io.github.mbarcia.pipeline.step.StepOneToMany;
import io.github.mbarcia.pipeline.step.StepOneToOne;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Java annotation processor that generates both gRPC client and server step implementations
 * based on @PipelineStep annotated service classes.
 */
@SupportedAnnotationTypes("io.github.mbarcia.pipeline.annotation.PipelineStep")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class PipelineStepProcessor extends AbstractProcessor {

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

    private void generateClientStep(TypeElement serviceClass, PipelineStep pipelineStep) throws IOException {
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
        TypeMirror grpcStubType = getAnnotationValue(annotationMirror, "grpcStub");
        String grpcClientName = getAnnotationValueAsString(annotationMirror, "grpcClient");
        TypeMirror inboundMapperType = getAnnotationValue(annotationMirror, "inboundMapper");
        TypeMirror outboundMapperType = getAnnotationValue(annotationMirror, "outboundMapper");
        TypeMirror inputGrpcType = getAnnotationValue(annotationMirror, "inputGrpcType");
        TypeMirror outputGrpcType = getAnnotationValue(annotationMirror, "outputGrpcType");
        TypeMirror stepType = getAnnotationValue(annotationMirror, "stepType");

        // Create the package for the generated class
        String packageName = processingEnv.getElementUtils()
            .getPackageOf(serviceClass).getQualifiedName().toString() + ".generated";
        
        // Create the simple name for the generated client step
        String serviceClassName = serviceClass.getSimpleName().toString();
        String clientStepClassName = serviceClassName.replace("Service", "") + "ClientStep";
        
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
        // The server-side GenericGrpcReactiveServiceAdapter handles gRPC-to-domain conversion
        ClassName configurableStep = ClassName.get("io.github.mbarcia.pipeline.step", "ConfigurableStep");
        clientStepBuilder.superclass(configurableStep);

        ClassName stepInterface;
        if (stepType != null && stepType.toString().equals("io.github.mbarcia.pipeline.step.StepOneToOne")) {
            stepInterface = ClassName.get(StepOneToOne.class);
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                ClassName.get(inputGrpcType), ClassName.get(outputGrpcType)));
        } else if (stepType != null && stepType.toString().equals("io.github.mbarcia.pipeline.step.StepOneToMany")) {
            stepInterface = ClassName.get(StepOneToMany.class);
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                ClassName.get(inputGrpcType), ClassName.get(outputGrpcType)));
        } else if (stepType != null && stepType.toString().equals("io.github.mbarcia.pipeline.step.StepManyToOne")) {
            // Handle StepManyToOne specifically
            stepInterface = ClassName.get("io.github.mbarcia.pipeline.step", "StepManyToOne");
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                ClassName.get(inputGrpcType), ClassName.get(outputGrpcType)));
        } else if (stepType != null && stepType.toString().equals("io.github.mbarcia.pipeline.step.StepManyToMany")) {
            // Handle StepManyToMany
            stepInterface = ClassName.get("io.github.mbarcia.pipeline.step", "StepManyToMany");
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                ClassName.get(inputGrpcType), ClassName.get(outputGrpcType)));
        } else {
            // Default to OneToOne for simplicity; can extend to support more types
            stepInterface = ClassName.get(StepOneToOne.class);
            clientStepBuilder.addSuperinterface(ParameterizedTypeName.get(stepInterface,
                ClassName.get(inputGrpcType), ClassName.get(outputGrpcType)));
        }
        
        // Add the apply method implementation based on the step type
        // No mapping needed - client steps work directly with gRPC types
        // Server-side GenericGrpcReactiveServiceAdapter handles gRPC-to-domain conversion
        if (stepType != null && stepType.toString().equals("io.github.mbarcia.pipeline.step.StepOneToMany")) {
            // For OneToMany: Input -> Multi<Output> (StepOneToMany interface has applyOneToMany(Input in) method)
            MethodSpec applyMethod = MethodSpec.methodBuilder("applyOneToMany")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Multi.class), 
                    ClassName.get(outputGrpcType)))
                .addParameter(ClassName.get(inputGrpcType), "input")
                .addStatement("return this.grpcClient.remoteProcess(input)")
                .build();
                
            clientStepBuilder.addMethod(applyMethod);
        } else if (stepType != null && stepType.toString().equals("io.github.mbarcia.pipeline.step.StepManyToOne")) {
            // For ManyToOne: Multi<Input> -> Uni<Output> (ManyToOne interface has applyBatchMulti(Multi<Input> in) method)
            MethodSpec applyMethod = MethodSpec.methodBuilder("applyBatchMulti")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Uni.class), 
                    ClassName.get(outputGrpcType)))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Multi.class), 
                    ClassName.get(inputGrpcType)), "inputs")
                // Client streaming gRPC requires special handling - placeholder implementation
                .addStatement("throw new UnsupportedOperationException(\"Client streaming gRPC requires special implementation - please implement manually\")")
                .build();
                
            clientStepBuilder.addMethod(applyMethod);
        } else {
            // Default to OneToOne: Input -> Uni<Output> (StepOneToOne interface has applyOneToOne(Input in) method)
            MethodSpec applyMethod = MethodSpec.methodBuilder("applyOneToOne")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Uni.class), 
                    ClassName.get(outputGrpcType)))
                .addParameter(ClassName.get(inputGrpcType), "input")
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

    private void generateGrpcServiceAdapter(TypeElement serviceClass, PipelineStep pipelineStep) throws IOException {
        // Get the annotation mirror to extract TypeMirror values
        AnnotationMirror annotationMirror = getAnnotationMirror(serviceClass, PipelineStep.class);
        if (annotationMirror == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, 
                "Could not get annotation mirror for " + serviceClass, serviceClass);
            return;
        }

        // Extract values from the annotation mirror
        TypeMirror inboundMapperType = getAnnotationValue(annotationMirror, "inboundMapper");
        TypeMirror outboundMapperType = getAnnotationValue(annotationMirror, "outboundMapper");
        String serviceName = serviceClass.getQualifiedName().toString();
        TypeMirror backendType = getAnnotationValue(annotationMirror, "backendType");
        boolean autoPersistenceEnabled = getAnnotationValueAsBoolean(annotationMirror, "autoPersist", true);

        // Use the same package as the original service but with a ".pipeline" suffix
        String fqcn = serviceClass.getQualifiedName().toString();
        String originalPackage = fqcn.substring(0, fqcn.lastIndexOf('.'));
        String pkg = String.format("%s.pipeline", originalPackage);
        String simpleClassName = String.format("%sGrpcService", serviceClass.getSimpleName().toString());

        // Determine the backend type (default to GenericGrpcReactiveServiceAdapter)
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
            backendClassName = ClassName.get(GenericGrpcReactiveServiceAdapter.class);
        }

        // Create the gRPC service class
        TypeSpec.Builder grpcServiceBuilder = TypeSpec.classBuilder(simpleClassName)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(ClassName.get(GrpcService.class)).build())
            .addAnnotation(AnnotationSpec.builder(ApplicationScoped.class).build())
            .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "NoArgsConstructor")).build())
            .addAnnotation(AnnotationSpec.builder(ClassName.get("lombok", "Getter")).build())
            .superclass(backendClassName);

        // Add field declarations
        if (inboundMapperType != null && !inboundMapperType.toString().equals("void")) {
            FieldSpec inboundMapperField = FieldSpec.builder(
                ClassName.get(inboundMapperType),
                "inboundMapper",
                Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Inject.class).build())
                .build();
            grpcServiceBuilder.addField(inboundMapperField);
        }

        if (outboundMapperType != null && !outboundMapperType.toString().equals("void")) {
            FieldSpec outboundMapperField = FieldSpec.builder(
                ClassName.get(outboundMapperType),
                "outboundMapper",
                Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Inject.class).build())
                .build();
            grpcServiceBuilder.addField(outboundMapperField);
        }

        FieldSpec serviceField = FieldSpec.builder(
            ClassName.get(serviceClass),
            "service",
            Modifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(Inject.class).build())
            .build();
        grpcServiceBuilder.addField(serviceField);

        // Add persistence manager field
        FieldSpec persistenceManagerField = FieldSpec.builder(
                ClassName.get("io.github.mbarcia.pipeline.persistence", "PersistenceManager"),
                "persistenceManager",
                Modifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(Inject.class).build())
            .build();
        grpcServiceBuilder.addField(persistenceManagerField);

        // Add method to determine auto-persistence
        MethodSpec autoPersistenceMethod = MethodSpec.methodBuilder("isAutoPersistenceEnabled")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PROTECTED)
            .returns(boolean.class)
            .addStatement("return $L", autoPersistenceEnabled)
            .build();
        grpcServiceBuilder.addMethod(autoPersistenceMethod);

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

    private AnnotationMirror getAnnotationMirror(Element element, Class<?> annotationClass) {
        String annotationClassName = annotationClass.getCanonicalName();
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotationClassName)) {
                return annotationMirror;
            }
        }
        return null;
    }

    private TypeMirror getAnnotationValue(AnnotationMirror annotation, String memberName) {
        for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
            if (executableElement.getSimpleName().toString().equals(memberName)) {
                return (TypeMirror) annotation.getElementValues().get(executableElement).getValue();
            }
        }
        return null;
    }

    private String getAnnotationValueAsString(AnnotationMirror annotation, String memberName) {
        for (ExecutableElement executableElement : annotation.getElementValues().keySet()) {
            if (executableElement.getSimpleName().toString().equals(memberName)) {
                return (String) annotation.getElementValues().get(executableElement).getValue();
            }
        }
        return null;
    }

    private boolean getAnnotationValueAsBoolean(AnnotationMirror annotation, String memberName, boolean defaultValue) {
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