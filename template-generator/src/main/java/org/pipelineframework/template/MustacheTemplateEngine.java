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

package org.pipelineframework.template;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MustacheTemplateEngine {
    
    public void generateApplication(String appName, String basePackage, List<Map<String, Object>> steps, Path outputPath) throws Exception {
        MustacheFactory mf = new DefaultMustacheFactory();
        
        // For sequential pipeline, update input types of steps after the first one
        // to match the output type of the previous step
        for (int i = 1; i < steps.size(); i++) {
            Map<String, Object> currentStep = steps.get(i);
            Map<String, Object> previousStep = steps.get(i - 1);
            // Set the input type of the current step to the output type of the previous step
            currentStep.put("inputTypeName", previousStep.get("outputTypeName"));
        }
        
        // Generate parent POM
        generateParentPom(mf, appName, basePackage, steps, outputPath);
        
        // Generate common module
        generateCommonModule(mf, appName, basePackage, steps, outputPath);
        
        // Generate each step service
        for (int i = 0; i < steps.size(); i++) {
            generateStepService(mf, appName, basePackage, steps.get(i), outputPath, i, steps);
        }
        
        // Generate orchestrator
        generateOrchestrator(mf, appName, basePackage, steps, outputPath);
        
        // Generate docker-compose
        generateDockerCompose(mf, appName, steps, outputPath);
        
        // Generate utility scripts
        generateUtilityScripts(mf, outputPath);
        
        // Generate observability configs
        generateObservabilityConfigs(mf, outputPath);
        
        // Generate mvnw files
        generateMvNWFiles(mf, outputPath);
        
        // Generate Maven wrapper files
        generateMavenWrapperFiles(outputPath);
        
        // Generate other files
        generateOtherFiles(mf, appName, outputPath);
    }
    
    private void generateParentPom(MustacheFactory mf, String appName, String basePackage, List<Map<String, Object>> steps, Path outputPath) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("basePackage", basePackage);
        context.put("artifactId", appName.toLowerCase().replaceAll("[^a-zA-Z0-9]", "-"));
        context.put("name", appName);
        context.put("steps", steps);
        
        Mustache mustache = mf.compile("templates/parent-pom.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Path pomPath = outputPath.resolve("pom.xml");
        Files.createDirectories(pomPath.getParent());
        Files.write(pomPath, stringWriter.toString().getBytes());
    }
    
    private void generateCommonModule(MustacheFactory mf, String appName, String basePackage, List<Map<String, Object>> steps, Path outputPath) throws IOException {
        Path commonPath = outputPath.resolve("common");
        Files.createDirectories(commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.domain")));
        Files.createDirectories(commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.dto")));
        Files.createDirectories(commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.mapper")));
        Files.createDirectories(commonPath.resolve("src/main/proto"));
        
        // Generate common POM
        generateCommonPom(mf, appName, basePackage, commonPath);
        
        // Generate proto files for each step
        for (int i = 0; i < steps.size(); i++) {
            generateProtoFile(mf, steps.get(i), basePackage, commonPath, i, steps);
        }
        
        // Generate entities, DTOs, and mappers for each step
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = steps.get(i);
            generateDomainClasses(mf, step, basePackage, commonPath, i);
            generateDtoClasses(mf, step, basePackage, commonPath, i);
            generateMapperClasses(mf, step, basePackage, commonPath, i);
        }
        
        // Generate base entity
        generateBaseEntity(mf, basePackage, commonPath);
        
        // Generate common converters
        generateCommonConverters(mf, basePackage, commonPath);
    }
    
    private void generateCommonPom(MustacheFactory mf, String appName, String basePackage, Path commonPath) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("basePackage", basePackage);
        context.put("rootProjectName", appName.toLowerCase().replaceAll("[^a-zA-Z0-9]", "-"));
        
        Mustache mustache = mf.compile("templates/common-pom.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Path pomPath = commonPath.resolve("pom.xml");
        Files.write(pomPath, stringWriter.toString().getBytes());
    }
    
    private void generateProtoFile(MustacheFactory mf, Map<String, Object> step, String basePackage, Path commonPath, int stepIndex, List<Map<String, Object>> allSteps) throws IOException {
        // Process input fields to add field numbers
        @SuppressWarnings("unchecked")
        List<Map<String, String>> inputFields = (List<Map<String, String>>) step.get("inputFields");
        for (int i = 0; i < inputFields.size(); i++) {
            inputFields.get(i).put("fieldNumber", String.valueOf(i + 1));
        }
        
        // Process output fields to add field numbers starting after input fields
        @SuppressWarnings("unchecked")
        List<Map<String, String>> outputFields = (List<Map<String, String>>) step.get("outputFields");
        int outputStartNumber = inputFields.size() + 1;
        for (int i = 0; i < outputFields.size(); i++) {
            outputFields.get(i).put("fieldNumber", String.valueOf(outputStartNumber + i));
        }
        
        Map<String, Object> context = new HashMap<>(step);
        context.put("basePackage", basePackage);
        context.put("isExpansion", "EXPANSION".equals(step.get("cardinality")));
        context.put("isReduction", "REDUCTION".equals(step.get("cardinality")));
        context.put("isFirstStep", stepIndex == 0);
        if (stepIndex > 0) {
            // Reference the previous step's input type as the current step's input type
            Map<String, Object> previousStep = allSteps.get(stepIndex - 1);
            context.put("previousStepName", previousStep.get("serviceName"));
            context.put("previousStepOutputTypeName", previousStep.get("outputTypeName"));
        }
        // Format the service name properly for the proto file (e.g., "Process Customer" -> "Customer", "Validate Order" -> "Order")
        String stepName = (String) step.get("name");
        String formattedName = stepName.replace("Process ", "").trim();
        // Remove spaces and capitalize each word (e.g., "Validate Order" -> "ValidateOrder")
        String[] parts = formattedName.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase());
            }
        }
        context.put("serviceNameFormatted", sb.toString());
        
        Mustache mustache = mf.compile("templates/proto.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Path protoPath = commonPath.resolve("src/main/proto/" + step.get("serviceName") + ".proto");
        Files.write(protoPath, stringWriter.toString().getBytes());
    }
    
    private void generateDomainClasses(MustacheFactory mf, Map<String, Object> step, String basePackage, Path commonPath, int stepIndex) throws IOException {
        // Process input domain class only for first step
        if (stepIndex == 0) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> inputFields = (List<Map<String, String>>) step.get("inputFields");
            Map<String, Object> inputContext = new HashMap<>(step);
            inputContext.put("basePackage", basePackage);
            inputContext.put("className", step.get("inputTypeName"));
            inputContext.put("fields", inputFields);
            addImportFlagsToContext(inputContext, inputFields);
            
            Mustache mustache = mf.compile("templates/domain.mustache");
            StringWriter stringWriter = new StringWriter();
            mustache.execute(stringWriter, inputContext);
            stringWriter.flush();
            
            Path inputDomainPath = commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.domain"))
                .resolve(step.get("inputTypeName") + ".java");
            Files.write(inputDomainPath, stringWriter.toString().getBytes());
        }
        
        // Process output domain class for all steps
        @SuppressWarnings("unchecked")
        List<Map<String, String>> outputFields = (List<Map<String, String>>) step.get("outputFields");
        Map<String, Object> outputContext = new HashMap<>(step);
        outputContext.put("basePackage", basePackage);
        outputContext.put("className", step.get("outputTypeName"));
        outputContext.put("fields", outputFields);
        addImportFlagsToContext(outputContext, outputFields);
        
        Mustache outputMustache = mf.compile("templates/domain.mustache");
        StringWriter outputStringWriter = new StringWriter();
        outputMustache.execute(outputStringWriter, outputContext);
        outputStringWriter.flush();
        
        Path outputDomainPath = commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.domain"))
            .resolve(step.get("outputTypeName") + ".java");
        Files.write(outputDomainPath, outputStringWriter.toString().getBytes());
    }
    
    private void generateBaseEntity(MustacheFactory mf, String basePackage, Path commonPath) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("basePackage", basePackage);
        
        Mustache mustache = mf.compile("templates/base-entity.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Path baseEntityPath = commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.domain"))
            .resolve("BaseEntity.java");
        Files.write(baseEntityPath, stringWriter.toString().getBytes());
    }
    
    private void generateDtoClasses(MustacheFactory mf, Map<String, Object> step, String basePackage, Path commonPath, int stepIndex) throws IOException {
        // Process input DTO class only for first step
        if (stepIndex == 0) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> inputFields = (List<Map<String, String>>) step.get("inputFields");
            Map<String, Object> inputContext = new HashMap<>(step);
            inputContext.put("basePackage", basePackage);
            inputContext.put("className", step.get("inputTypeName") + "Dto");
            inputContext.put("fields", inputFields);
            addImportFlagsToContext(inputContext, inputFields);
            
            Mustache mustache = mf.compile("templates/dto.mustache");
            StringWriter stringWriter = new StringWriter();
            mustache.execute(stringWriter, inputContext);
            stringWriter.flush();
            
            Path inputDtoPath = commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.dto"))
                .resolve(step.get("inputTypeName") + "Dto.java");
            Files.write(inputDtoPath, stringWriter.toString().getBytes());
        }
        
        // Process output DTO class for all steps
        @SuppressWarnings("unchecked")
        List<Map<String, String>> outputFields = (List<Map<String, String>>) step.get("outputFields");
        Map<String, Object> outputContext = new HashMap<>(step);
        outputContext.put("basePackage", basePackage);
        outputContext.put("className", step.get("outputTypeName") + "Dto");
        outputContext.put("fields", outputFields);
        addImportFlagsToContext(outputContext, outputFields);
        
        Mustache outputMustache = mf.compile("templates/dto.mustache");
        StringWriter outputStringWriter = new StringWriter();
        outputMustache.execute(outputStringWriter, outputContext);
        outputStringWriter.flush();
        
        Path outputDtoPath = commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.dto"))
            .resolve(step.get("outputTypeName") + "Dto.java");
        Files.write(outputDtoPath, outputStringWriter.toString().getBytes());
    }
    
    private void generateMapperClasses(MustacheFactory mf, Map<String, Object> step, String basePackage, Path commonPath, int stepIndex) throws IOException {
        // Generate input mapper class only for first step (since other steps reference previous step's output)
        if (stepIndex == 0) {
            generateMapperClass(mf, step.get("inputTypeName").toString(), step, basePackage, commonPath);
        }
        
        // Generate output mapper class for all steps
        generateMapperClass(mf, step.get("outputTypeName").toString(), step, basePackage, commonPath);
    }
    
    private void generateMapperClass(MustacheFactory mf, String className, Map<String, Object> step, String basePackage, Path commonPath) throws IOException {
        Map<String, Object> context = new HashMap<>(step);
        context.put("basePackage", basePackage);
        context.put("className", className);
        context.put("domainClass", className.replace("Dto", ""));
        context.put("dtoClass", className + "Dto");
        // Convert service name to proper format for proto-generated class
        String protoClassName = formatForProtoClassName((String) step.get("serviceName"));
        context.put("grpcClass", basePackage + ".grpc." + protoClassName);
        
        Mustache mustache = mf.compile("templates/mapper.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Path mapperPath = commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.mapper"))
            .resolve(className + "Mapper.java");
        Files.write(mapperPath, stringWriter.toString().getBytes());
    }
    
    private void generateCommonConverters(MustacheFactory mf, String basePackage, Path commonPath) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("basePackage", basePackage);
        
        Mustache mustache = mf.compile("templates/common-converters.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Path convertersPath = commonPath.resolve("src/main/java").resolve(toPath(basePackage + ".common.mapper"))
            .resolve("CommonConverters.java");
        Files.write(convertersPath, stringWriter.toString().getBytes());
    }
    
    private void generateStepService(MustacheFactory mf, String appName, String basePackage, Map<String, Object> step, Path outputPath, int stepIndex, List<Map<String, Object>> allSteps) throws IOException {
        Path stepPath = outputPath.resolve((String)step.get("serviceName"));
        // Convert hyphens to underscores for valid Java package names
        String serviceNameForPackage = ((String) step.get("serviceName")).toString().replace("-svc", "").replace('-', '_');
        Files.createDirectories(stepPath.resolve("src/main/java").resolve(toPath(basePackage + "." + serviceNameForPackage + ".service")));
        
        // Add rootProjectName to step map
        step.put("rootProjectName", appName.toLowerCase().replaceAll("[^a-zA-Z0-9]", "-"));
        
        // Generate step POM
        generateStepPom(mf, step, basePackage, stepPath);
        
        // Generate the service class
        generateStepServiceClass(mf, appName, basePackage, step, stepPath, stepIndex, allSteps);
        
        // Generate Dockerfile
        generateDockerfile(mf, step.get("serviceName").toString(), stepPath);
    }
    
    private void generateStepPom(MustacheFactory mf, Map<String, Object> step, String basePackage, Path stepPath) throws IOException {
        Map<String, Object> context = new HashMap<>(step);
        context.put("basePackage", basePackage);
        context.put("artifactId", step.get("serviceName"));
        context.put("rootProjectName", step.get("rootProjectName"));
        
        Mustache mustache = mf.compile("templates/step-pom.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Files.write(stepPath.resolve("pom.xml"), stringWriter.toString().getBytes());
    }
    
    private void generateStepServiceClass(MustacheFactory mf, String appName, String basePackage, Map<String, Object> step, Path stepPath, int stepIndex, List<Map<String, Object>> allSteps) throws IOException {
        Map<String, Object> context = new HashMap<>(step);
        context.put("basePackage", basePackage);
        context.put("serviceName", step.get("serviceName").toString().replace("-svc", ""));
        // Convert hyphens to underscores for valid Java package names
        String serviceNameForPackage = ((String) step.get("serviceName")).toString().replace("-svc", "").replace('-', '_');
        context.put("serviceNameForPackage", serviceNameForPackage);
        
        // Format service name for proto-generated class names (e.g., "process-customer-svc" -> "ProcessCustomerSvc")
        String protoClassName = formatForProtoClassName((String) step.get("serviceName"));
        context.put("protoClassName", protoClassName);
        
        // Determine the inputGrpcType proto class name based on step position
        if (stepIndex == 0) {
            // For the first step, inputGrpcType comes from the same proto file
            context.put("inputGrpcProtoClassName", protoClassName);
        } else {
            // For subsequent steps, inputGrpcType comes from the previous step's proto file
            Map<String, Object> previousStep = allSteps.get(stepIndex - 1);
            String previousProtoClassName = formatForProtoClassName((String) previousStep.get("serviceName"));
            context.put("inputGrpcProtoClassName", previousProtoClassName);
        }
        
        // Use the serviceNameCamel field from the configuration to form the gRPC class names
        String serviceNameCamel = (String) step.get("serviceNameCamel");
        // Convert camelCase to PascalCase (e.g., "validateOrder" -> "ValidateOrder", "processCustomer" -> "ProcessCustomer")
        String serviceNamePascal = Character.toUpperCase(serviceNameCamel.charAt(0)) + serviceNameCamel.substring(1);
        
        // Extract the entity name from the PascalCase service name to match proto service names
        // ProcessCustomer -> Customer, ValidateOrder -> Order
        String entityName = extractEntityName(serviceNamePascal);
        
        // For gRPC class names, use the pattern MutinyProcess[Entity]ServiceGrpc to match proto generation
        // ProcessCustomerService proto -> MutinyProcessCustomerServiceGrpc
        // ProcessValidateOrderService proto -> MutinyProcessValidateOrderServiceGrpc
        String grpcServiceName = "MutinyProcess" + entityName + "ServiceGrpc";
        String grpcStubName = grpcServiceName + ".MutinyProcess" + entityName + "ServiceStub";
        String grpcImplName = grpcServiceName + ".Process" + entityName + "ServiceImplBase";
        
        context.put("grpcServiceName", grpcServiceName);
        context.put("grpcStubName", grpcStubName);
        context.put("grpcImplName", grpcImplName);
        context.put("serviceNamePascal", serviceNamePascal);
        context.put("serviceNameFormatted", step.get("name"));
        
        String reactiveServiceInterface = "ReactiveService";
        String grpcAdapter = "GrpcReactiveServiceAdapter";
        String processMethodReturnType = "Uni<" + step.get("outputTypeName") + ">";
        String processMethodParamType = (String) step.get("inputTypeName");
        String returnStatement = "Uni.createFrom().item(output)";
        
        if ("EXPANSION".equals(step.get("cardinality"))) {
            reactiveServiceInterface = "ReactiveStreamingService";
            grpcAdapter = "GrpcServiceStreamingAdapter";
            processMethodReturnType = "Multi<" + step.get("outputTypeName") + ">";
            returnStatement = "Multi.createFrom().item(output)";
        } else if ("REDUCTION".equals(step.get("cardinality"))) {
            reactiveServiceInterface = "ReactiveStreamingClientService";
            grpcAdapter = "GrpcServiceClientStreamingAdapter";
            processMethodParamType = "Multi<" + step.get("inputTypeName") + ">";
            returnStatement = "Uni.createFrom().item(output)";
        } else if ("SIDE_EFFECT".equals(step.get("cardinality"))) {
            reactiveServiceInterface = "ReactiveService";
            grpcAdapter = "GrpcReactiveServiceAdapter";
            returnStatement = "Uni.createFrom().item(input)";
        }
        
        context.put("reactiveServiceInterface", reactiveServiceInterface);
        context.put("grpcAdapter", grpcAdapter);
        context.put("processMethodReturnType", processMethodReturnType);
        context.put("processMethodParamType", processMethodParamType);
        context.put("returnStatement", returnStatement);
        
        Mustache mustache = mf.compile("templates/step-service.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        // Convert hyphens to underscores for valid Java package names  
        String stepServiceNameForPackage = ((String) step.get("serviceName")).toString().replace("-svc", "").replace('-', '_');
        Path servicePath = stepPath.resolve("src/main/java").resolve(toPath(basePackage + "." + stepServiceNameForPackage + ".service"))
            .resolve("Process" + serviceNamePascal + "Service.java");
        Files.write(servicePath, stringWriter.toString().getBytes());
    }
    
    private void generateDockerfile(MustacheFactory mf, String serviceName, Path stepPath) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("serviceName", serviceName);
        
        Mustache mustache = mf.compile("templates/dockerfile.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Files.write(stepPath.resolve("Dockerfile"), stringWriter.toString().getBytes());
    }
    
    private void generateOrchestrator(MustacheFactory mf, String appName, String basePackage, List<Map<String, Object>> steps, Path outputPath) throws IOException {
        Path orchPath = outputPath.resolve("orchestrator-svc");
        Files.createDirectories(orchPath.resolve("src/main/java").resolve(toPath(basePackage + ".orchestrator.service")));
        
        // Generate orchestrator POM
        generateOrchestratorPom(mf, appName, basePackage, orchPath);
        
        // Generate Dockerfile
        generateDockerfile(mf, "orchestrator-svc", orchPath);
    }
    
    private void generateOrchestratorPom(MustacheFactory mf, String appName, String basePackage, Path orchPath) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("basePackage", basePackage);
        context.put("artifactId", "orchestrator-svc");
        context.put("rootProjectName", appName.toLowerCase().replaceAll("[^a-zA-Z0-9]", "-"));
        
        Mustache mustache = mf.compile("templates/orchestrator-pom.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Files.write(orchPath.resolve("pom.xml"), stringWriter.toString().getBytes());
    }
    
    private void generateDockerCompose(MustacheFactory mf, String appName, List<Map<String, Object>> steps, Path outputPath) throws IOException {
        // Process steps to add additional properties
        List<Map<String, Object>> processedSteps = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = new HashMap<>(steps.get(i));
            // Port number starting from 8444 for first service
            step.put("portNumber", 8444 + i);
            // Convert service name to uppercase with hyphens replaced by underscores
            step.put("serviceNameUpperCase", ((String)step.get("serviceName")).toUpperCase().replace('-', '_'));
            processedSteps.add(step);
        }
        
        Map<String, Object> context = new HashMap<>();
        context.put("appName", appName);
        context.put("steps", processedSteps);
        
        Mustache mustache = mf.compile("templates/docker-compose.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Files.write(outputPath.resolve("docker-compose.yml"), stringWriter.toString().getBytes());
    }
    
    private void generateUtilityScripts(MustacheFactory mf, Path outputPath) throws IOException {
        // Generate up-docker.sh
        Mustache mustache = mf.compile("templates/up-docker.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("up-docker.sh"), stringWriter.toString().getBytes());
        outputPath.resolve("up-docker.sh").toFile().setExecutable(true);
        
        // Generate down-docker.sh
        mustache = mf.compile("templates/down-docker.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("down-docker.sh"), stringWriter.toString().getBytes());
        outputPath.resolve("down-docker.sh").toFile().setExecutable(true);
        
        // Generate up-local.sh
        mustache = mf.compile("templates/up-local.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("up-local.sh"), stringWriter.toString().getBytes());
        
        // Generate down-local.sh
        mustache = mf.compile("templates/down-local.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("down-local.sh"), stringWriter.toString().getBytes());
    }
    
    private void generateObservabilityConfigs(MustacheFactory mf, Path outputPath) throws IOException {
        // Generate otel-collector-config.yaml
        Mustache mustache = mf.compile("templates/otel-collector-config.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("otel-collector-config.yaml"), stringWriter.toString().getBytes());
        
        // Generate prometheus.yml
        mustache = mf.compile("templates/prometheus.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("prometheus.yml"), stringWriter.toString().getBytes());
        
        // Generate grafana datasources
        mustache = mf.compile("templates/grafana-datasources.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("grafana-datasources.yaml"), stringWriter.toString().getBytes());
        
        // Generate grafana dashboards
        mustache = mf.compile("templates/grafana-dashboards.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("grafana-dashboards.yaml"), stringWriter.toString().getBytes());
        
        // Generate tempo config
        mustache = mf.compile("templates/tempo.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("tempo.yaml"), stringWriter.toString().getBytes());
    }
    
    private void generateMvNWFiles(MustacheFactory mf, Path outputPath) throws IOException {
        // Create mvnw (Unix)
        Mustache mustache = mf.compile("templates/mvnw.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("mvnw"), stringWriter.toString().getBytes());
        outputPath.resolve("mvnw").toFile().setExecutable(true);
        
        // Create mvnw.cmd (Windows)
        mustache = mf.compile("templates/mvnw-cmd.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve("mvnw.cmd"), stringWriter.toString().getBytes());
    }
    
    private void generateMavenWrapperFiles(Path outputPath) throws IOException {
        // Create .mvn/wrapper directory
        Path wrapperDir = outputPath.resolve(".mvn/wrapper");
        Files.createDirectories(wrapperDir);
        
        // Copy maven-wrapper.properties
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("templates/.mvn/wrapper/maven-wrapper.properties")) {
            if (is != null) {
                Files.copy(is, wrapperDir.resolve("maven-wrapper.properties"));
            }
        }
        
        // Note: We don't copy maven-wrapper.jar - the mvnw script will download it automatically when needed
    }
    
    private void generateOtherFiles(MustacheFactory mf, String appName, Path outputPath) throws IOException {
        // Create README
        Map<String, Object> context = new HashMap<>();
        context.put("appName", appName);
        
        Mustache mustache = mf.compile("templates/readme.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        Files.write(outputPath.resolve("README.md"), stringWriter.toString().getBytes());
        
        // Create .gitignore
        mustache = mf.compile("templates/gitignore.mustache");
        stringWriter = new StringWriter();
        mustache.execute(stringWriter, new HashMap<>());
        stringWriter.flush();
        Files.write(outputPath.resolve(".gitignore"), stringWriter.toString().getBytes());
    }
    
    private String toPath(String packageName) {
        return packageName.replace('.', '/');
    }
    
    private void addImportFlagsToContext(Map<String, Object> context, List<Map<String, String>> fields) {
        boolean hasDateFields = false;
        boolean hasBigIntegerFields = false;
        boolean hasBigDecimalFields = false;
        boolean hasCurrencyFields = false;
        boolean hasPathFields = false;
        boolean hasNetFields = false;
        boolean hasIoFields = false;
        boolean hasAtomicFields = false;
        boolean hasUtilFields = false;
        boolean hasIdField = false; // Check if there's already an 'id' field
        
        // Process fields and add list type information
        List<Map<String, String>> processedFields = new ArrayList<>();
        for (Map<String, String> field : fields) {
            Map<String, String> processedField = new HashMap<>(field);
            
            // Check if this is a list type and handle it appropriately
            String type = field.get("type");
            if ("List<String>".equals(type)) {
                processedField.put("isListType", "true");
                processedField.put("listInnerType", "string");
            } else {
                processedField.put("isListType", "false");
            }
            
            // Check if this field is named 'id'
            String fieldName = field.get("name");
            if ("id".equals(fieldName)) {
                processedField.put("isIdField", "true");
                hasIdField = true;
            } else {
                processedField.put("isIdField", "false");
            }
            
            processedFields.add(processedField);
            
            if (type != null) {
                switch (type) {
                    case "LocalDate":
                    case "LocalDateTime":
                    case "OffsetDateTime":
                    case "ZonedDateTime":
                    case "Instant":
                    case "Duration":
                    case "Period":
                        hasDateFields = true;
                        break;
                    case "BigInteger":
                        hasBigIntegerFields = true;
                        break;
                    case "BigDecimal":
                        hasBigDecimalFields = true;
                        break;
                    case "Currency":
                        hasCurrencyFields = true;
                        break;
                    case "Path":
                        hasPathFields = true;
                        break;
                    case "URI":
                    case "URL":
                        hasNetFields = true;
                        break;
                    case "File":
                        hasIoFields = true;
                        break;
                    case "AtomicInteger":
                    case "AtomicLong":
                        hasAtomicFields = true;
                        break;
                    case "List<String>":
                        hasUtilFields = true;
                        break;
                }
            }
        }
        
        context.put("fields", processedFields);
        context.put("hasDateFields", hasDateFields);
        context.put("hasBigIntegerFields", hasBigIntegerFields);
        context.put("hasBigDecimalFields", hasBigDecimalFields);
        context.put("hasCurrencyFields", hasCurrencyFields);
        context.put("hasPathFields", hasPathFields);
        context.put("hasNetFields", hasNetFields);
        context.put("hasIoFields", hasIoFields);
        context.put("hasAtomicFields", hasAtomicFields);
        context.put("hasUtilFields", hasUtilFields);
        context.put("hasIdField", hasIdField); // Add flag for existing id field
    }
    
    private String formatForClassName(String input) {
        // Split by spaces and capitalize each word
        String[] parts = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
    
    private String formatForProtoClassName(String input) {
        // Convert service names like "process-customer-svc" to "ProcessCustomerSvc"
        String[] parts = input.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                  .append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
    
    /**
     * Extracts the entity name from a PascalCase service name.
     * For example: "ProcessCustomer" -> "Customer", "ValidateOrder" -> "Order"
     */
    private String extractEntityName(String serviceNamePascal) {
        // If it starts with "Process", return everything after "Process"
        if (serviceNamePascal.startsWith("Process")) {
            return serviceNamePascal.substring("Process".length());
        }
        // For other cases, we'll default to the whole string
        // In practice for this framework, service names should start with "Process"
        return serviceNamePascal;
    }
}