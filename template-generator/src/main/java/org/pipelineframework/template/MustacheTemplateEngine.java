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
    
    /**
     * Generates a multi-module Java project from templates for the given application and pipeline steps.
     *
     * Builds the project structure (parent POM, common module, per-step services, orchestrator, Docker and observability configs,
     * utility scripts, Maven wrapper and other supporting files) under the specified output path and wires the pipeline by
     * updating each step's `inputTypeName` (for steps after the first) to the previous step's `outputTypeName`.
     *
     * @param appName     the application name used as the artifact/project name
     * @param basePackage the base Java package to use for generated sources
     * @param steps       a list of step definitions (maps) describing each pipeline step; entries are modified in-place
     *                    to set downstream `inputTypeName` values for sequential wiring
     * @param outputPath  the filesystem directory where the generated project will be written
     * @throws Exception  if any I/O, templating, or generation error occurs during project generation
     */
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
    
    /**
     * Generate the "common" Maven module and its artifacts (parent POM, proto files, domain classes,
     * DTOs, mappers, base entity, and common converters) under the given output path.
     *
     * @param mf the MustacheFactory used to compile templates
     * @param appName the root application name used in generated POMs and templates
     * @param basePackage the base Java package used for generated source packages
     * @param steps the ordered list of step descriptors; each map supplies data for proto, domain, DTO and mapper generation
     * @param outputPath the project root directory where the `common` module will be created
     * @throws IOException if writing files or creating directories fails
     */
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
    
    /**
     * Generates the common module's pom.xml from the common-pom.mustache template and writes it to the given common module path.
     *
     * @param mf the MustacheFactory used to compile templates
     * @param appName the root application name (used to derive the root project/artifact name)
     * @param basePackage the base Java package to place in the generated POM
     * @param commonPath filesystem path to the common module where pom.xml will be written
     * @throws IOException if template rendering or writing the pom.xml file fails
     */
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
    
    /**
     * Generate the .proto file for a pipeline step and write it to the common proto directory.
     *
     * The method assigns sequential protobuf field numbers to the step's input and output fields,
     * builds a template context (including flags `isExpansion`, `isReduction`, `isFirstStep` and,
     * when applicable, `previousStepName` and `previousStepOutputTypeName`), formats a proto-safe
     * service name as `serviceNameFormatted`, renders the `templates/proto.mustache` template with
     * that context, and writes the result to {@code commonPath/src/main/proto/{serviceName}.proto}.
     *
     * @param mf the MustacheFactory used to compile and execute the proto template
     * @param step a map describing the step; expected keys include:
     *             - "inputFields": List<Map<String,String>> of input field definitions
     *             - "outputFields": List<Map<String,String>> of output field definitions
     *             - "cardinality": step cardinality (e.g., "EXPANSION", "REDUCTION")
     *             - "name": human-readable step name (used to derive serviceNameFormatted)
     *             - "serviceName": artifact name used for the proto filename
     * @param basePackage the root Java package to include in the proto template context
     * @param commonPath filesystem path to the common module where proto files are written
     * @param stepIndex index of this step within the overall steps list (0-based); when greater than 0,
     *                  the previous step's output type is added to the template context
     * @param allSteps the full list of step maps; used to obtain previous-step metadata when stepIndex > 0
     * @throws IOException if rendering or writing the proto file fails
     */
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
    
    /**
     * Generates domain Java classes for a pipeline step: the input domain class for the first step and the output domain class for every step,
     * and writes them to the common module's domain package under the provided commonPath.
     *
     * The provided `step` map is expected to contain the keys `inputFields`, `inputTypeName`, `outputFields`, and `outputTypeName`;
     * `inputFields`/`outputFields` are lists of field maps used to populate the template. When `stepIndex` is 0 the input domain class is generated;
     * the output domain class is always generated. Both classes are written to
     * commonPath/src/main/java/{basePackage}.common.domain/{ClassName}.java.
     *
     * @param mf the MustacheFactory used to compile domain templates
     * @param step a map describing the step (must include `inputFields`, `inputTypeName`, `outputFields`, `outputTypeName`)
     * @param basePackage the base Java package used to form the target package for generated classes
     * @param commonPath path to the common module where domain sources will be written
     * @param stepIndex index of the step (0 indicates the first step)
     * @throws IOException if writing generated files fails
     */
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
    
    /**
     * Generate the BaseEntity Java source file in the common domain package.
     *
     * @param basePackage the root Java package used to locate the common.domain package
     * @param commonPath  path to the common module root where src/main/java/... will be created
     * @throws IOException if writing the generated BaseEntity.java file fails
     */
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
    
    /**
     * Generate DTO Java classes for the given step and write them into the common module.
     *
     * For stepIndex == 0 this generates an input DTO (named `{inputTypeName}Dto`) using the
     * step's `inputFields`. For every step this generates an output DTO (named `{outputTypeName}Dto`)
     * using the step's `outputFields`. Each DTO is rendered from the `templates/dto.mustache`
     * template and written to
     * `commonPath/src/main/java/{basePackage}.common.dto/{TypeName}Dto.java`.
     *
     * @param mf the MustacheFactory used to compile templates
     * @param step a map containing step metadata; expected keys used here include
     *             `inputFields`, `outputFields`, `inputTypeName`, and `outputTypeName`
     * @param basePackage the root Java package for generated sources (used to build target path and package declarations)
     * @param commonPath filesystem path to the common module root where DTO files should be written
     * @param stepIndex index of the step in the pipeline; when zero, an input DTO is generated in addition to the output DTO
     * @throws IOException if template execution or writing files to disk fails
     */
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
    
    /**
     * Generates mapper classes for a given step: always generates the output mapper and,
     * for the first step only, also generates the input mapper.
     *
     * The provided step map must contain the keys `inputTypeName` and `outputTypeName`.
     *
     * @param mf the MustacheFactory used to compile templates
     * @param step a map describing the step; must include `inputTypeName` and `outputTypeName`
     * @param basePackage the base Java package used when rendering templates
     * @param commonPath filesystem path to the common module where mapper sources are written
     * @param stepIndex the zero-based index of the step; when zero, an input mapper is generated in addition to the output mapper
     * @throws IOException if writing generated files fails
     */
    private void generateMapperClasses(MustacheFactory mf, Map<String, Object> step, String basePackage, Path commonPath, int stepIndex) throws IOException {
        // Generate input mapper class only for first step (since other steps reference previous step's output)
        if (stepIndex == 0) {
            generateMapperClass(mf, step.get("inputTypeName").toString(), step, basePackage, commonPath);
        }
        
        // Generate output mapper class for all steps
        generateMapperClass(mf, step.get("outputTypeName").toString(), step, basePackage, commonPath);
    }
    
    /**
     * Generate a Java mapper class file for the given DTO/domain using the mapper template.
     *
     * Produces a mapper source file under the common module's mapper package using the provided
     * Mustache template and writes it to disk.
     *
     * @param mf the MustacheFactory used to compile templates
     * @param className the DTO or domain class base name (e.g., `OrderDto`); the domain class name is derived by removing the `Dto` suffix
     * @param step a context map describing the step (serviceName and related metadata) used to populate the template
     * @param basePackage the project's base Java package (used to build the mapper package and grpc class reference)
     * @param commonPath filesystem path to the common module where the generated mapper file will be written
     * @throws IOException if writing the generated mapper file fails
     */
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
    
    /**
     * Generates the CommonConverters Java source file in the common module from the common-converters.mustache template.
     *
     * @param basePackage the base Java package used to compute the target package for the generated class
     * @param commonPath  filesystem path to the common module root where src/main/java/.../CommonConverters.java will be written
     * @throws IOException if template compilation or writing the generated file fails
     */
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
    
    /**
     * Generates a service module for a pipeline step: creates package directories, writes the step POM,
     * renders and writes the step service class, and generates the step Dockerfile.
     *
     * @param mf the MustacheFactory used to compile and execute templates
     * @param appName the application root name used for artifact/project naming
     * @param basePackage the base Java package for generated sources
     * @param step a context map for the step; must contain at least a "serviceName" and may be
     *             augmented with generation metadata (e.g., artifact ids, type names)
     * @param outputPath the root output path for the generated multi-module project
     * @param stepIndex the zero-based index of this step within the steps list
     * @param allSteps the full list of step context maps (used when generating cross-step references)
     * @throws IOException if filesystem operations or template rendering output fail
     */
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
    
    /**
     * Generate the Maven POM for a step service module from the step-pom.mustache template.
     *
     * @param mf the MustacheFactory used to compile and execute templates
     * @param step a map of step metadata; expected keys include `serviceName` (artifactId) and `rootProjectName`
     * @param basePackage the base Java package to include in the generated POM
     * @param stepPath the filesystem path of the step module where `pom.xml` will be written
     * @throws IOException if an I/O error occurs writing the generated POM file
     */
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
    
    /**
     * Generate the Java service class for a single processing step and write it to the step module.
     *
     * Builds a template context from the provided step configuration and generation parameters, renders
     * the step-service.mustache template, and writes the resulting Process{ServiceNamePascal}Service.java
     * into the step module's service package.
     *
     * @param mf           the MustacheFactory used to compile templates
     * @param appName      the root application name used in generated artifacts
     * @param basePackage  the base Java package for the generated project
     * @param step         a map containing the step configuration (expected keys include
     *                     "serviceName", "serviceNameCamel", "name", "inputTypeName", "outputTypeName",
     *                     and "cardinality")
     * @param stepPath     filesystem path to the step module where the generated file will be written
     * @param stepIndex    zero-based index of this step within the overall pipeline; influences input proto resolution
     * @param allSteps     list of all step configuration maps (used to reference the previous step when stepIndex > 0)
     * @throws IOException if writing the generated service file fails
     */
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
        Files.createDirectories(orchPath.resolve("src/main/java").resolve(toPath(basePackage + ".orchestrator")));
        
        // Generate orchestrator POM
        generateOrchestratorPom(mf, appName, basePackage, orchPath);
        
        // Generate orchestrator application class
        generateOrchestratorApplication(mf, appName, basePackage, steps, orchPath);
        
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
    
    private void generateOrchestratorApplication(MustacheFactory mf, String appName, String basePackage, List<Map<String, Object>> steps, Path orchPath) throws IOException {
        // Prepare context for the template
        Map<String, Object> context = new HashMap<>();
        context.put("basePackage", basePackage);
        context.put("appName", appName);
        
        // Use the first step for the input type to the orchestrator
        if (!steps.isEmpty()) {
            Map<String, Object> firstStep = steps.get(0);
            context.put("firstInputTypeName", firstStep.get("inputTypeName"));
            
            // Format service name for proto class name (e.g., "process-customer-svc" -> "ProcessCustomerSvc")
            String firstStepServiceName = (String) firstStep.get("serviceName");
            String firstStepProtoClassName = formatForProtoClassName(firstStepServiceName);
            context.put("firstStepProtoClassName", firstStepProtoClassName);
        }
        
        Mustache mustache = mf.compile("templates/orchestrator-application.mustache");
        StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, context);
        stringWriter.flush();
        
        Path appPath = orchPath.resolve("src/main/java").resolve(toPath(basePackage + ".orchestrator"))
            .resolve("OrchestratorApplication.java");
        Files.write(appPath, stringWriter.toString().getBytes());
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
    
    /**
     * Analyze field definitions and populate the template context with a processed field list and import/type flags.
     *
     * Processes each entry in `fields`, annotating each field map with:
     * - `isListType` (`true`/`false`) and `listInnerType` when applicable
     * - `isIdField` (`true`/`false`)
     *
     * Also sets boolean flags in `context` indicating whether any field requires specific imports or handling:
     * `hasDateFields`, `hasBigIntegerFields`, `hasBigDecimalFields`, `hasCurrencyFields`, `hasPathFields`,
     * `hasNetFields`, `hasIoFields`, `hasAtomicFields`, `hasUtilFields`, and `hasIdField`.
     *
     * @param context the template context to populate (will receive `fields` and the boolean flags)
     * @param fields  a list of field descriptor maps; each map is expected to contain at least `"name"` and `"type"` (the type may be null)
     */
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
    
    /**
     * Convert a space-separated string into a PascalCase class name.
     *
     * @param input the input string containing words separated by spaces (may be mixed case)
     * @return the concatenation of words with each word capitalized (first letter upper-case, remaining letters lower-case)
     */
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
    
    /**
     * Convert a hyphen-separated service name into a PascalCase identifier suitable for proto class names.
     *
     * @param input the service name using hyphens (e.g., "process-customer-svc")
     * @return the PascalCase class name (e.g., "ProcessCustomerSvc")
     */
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