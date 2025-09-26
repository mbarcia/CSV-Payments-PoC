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

package io.github.mbarcia.template;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "template-generator", mixinStandardHelpOptions = true, version = "1.0.0", description = "Pipeline Template Generator - Creates a new pipeline application from a template")
public class TemplateGeneratorCli implements Callable<Integer> {

    @Option(names = {"-o", "--output"}, description = "Output directory for generated application")
    private String outputDir = "./generated-app";

    @Override
    public Integer call() throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Welcome to the Pipeline Template Generator!");
        System.out.println("==========================================");
        
        // Collect basic application info
        System.out.print("Enter the name of your application: ");
        String appName = scanner.nextLine().trim();
        
        System.out.print("Enter the base package name (e.g., io.github.mbarcia): ");
        String basePackage = scanner.nextLine().trim();
        
        // Collect step information
        List<Map<String, Object>> steps = collectSteps(scanner);
        
        // Create the application structure
        Path outputPath = Paths.get(outputDir).toAbsolutePath();
        Files.createDirectories(outputPath);
        
        // Create the template engine and generate all components
        MustacheTemplateEngine engine = new MustacheTemplateEngine();
        engine.generateApplication(appName, basePackage, steps, outputPath);
        
        System.out.println("\nApplication generated successfully in: " + outputDir);
        System.out.println("Run 'cd " + outputDir + " && ./mvnw clean compile' to verify the generated application.");
        
        return 0;
    }
    
    private List<Map<String, Object>> collectSteps(Scanner scanner) {
        List<Map<String, Object>> steps = new ArrayList<>();
        int stepNumber = 1;
        
        while (true) {
            System.out.println("\nStep " + stepNumber + ":");
            System.out.print("Enter step name (or press Enter to finish): ");
            String stepName = scanner.nextLine().trim();
            
            if (stepName.isEmpty()) {
                break;
            }
            
            System.out.println("Select cardinality for step '" + stepName + "':");
            System.out.println("1) 1-1 (One-to-One)");
            System.out.println("2) Expansion (1-Many)");
            System.out.println("3) Reduction (Many-1)");
            System.out.println("4) Side-effect-only 1-1");
            
            System.out.print("Enter choice (1-4): ");
            String cardinalityChoice = scanner.nextLine().trim();
            
            String cardinality = "";
            String stepType = "";
            switch (cardinalityChoice) {
                case "1": 
                    cardinality = "ONE_TO_ONE"; 
                    stepType = "StepOneToOne";
                    break;
                case "2": 
                    cardinality = "EXPANSION"; 
                    stepType = "StepOneToMany";
                    break;
                case "3": 
                    cardinality = "REDUCTION"; 
                    stepType = "StepManyToOne";
                    break;
                case "4": 
                    cardinality = "SIDE_EFFECT"; 
                    stepType = "StepOneToOne";
                    break;
                default: 
                    System.out.println("Invalid choice, defaulting to 1-1");
                    cardinality = "ONE_TO_ONE";
                    stepType = "StepOneToOne";
                    break;
            }
            
            System.out.print("Enter input type name (plural if reduction): ");
            String inputTypeName = scanner.nextLine().trim();
            
            // Collect input fields
            List<Map<String, String>> inputFields = collectFields(scanner, "input");
            
            System.out.print("Enter output type name (plural if expansion): ");
            String outputTypeName = scanner.nextLine().trim();
            
            // Collect output fields
            List<Map<String, String>> outputFields = collectFields(scanner, "output");
            
            // Create step definition
            Map<String, Object> step = new HashMap<>();
            step.put("name", stepName);
            step.put("serviceName", stepName.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase() + "-svc");
            step.put("serviceNameCamel", toCamelCase(stepName.replaceAll("[^a-zA-Z0-9]", " ")));
            step.put("cardinality", cardinality);
            step.put("stepType", stepType);
            step.put("inputTypeName", inputTypeName);
            step.put("inputTypeSimpleName", inputTypeName.replaceAll(".*\\.", ""));
            step.put("inputFields", inputFields);
            step.put("outputTypeName", outputTypeName);
            step.put("outputTypeSimpleName", outputTypeName.replaceAll(".*\\.", ""));
            step.put("outputFields", outputFields);
            step.put("order", stepNumber);
            step.put("grpcClientName", stepName.replaceAll("[^a-zA-Z0-9]", "") + "Svc");
            
            steps.add(step);
            stepNumber++;
        }
        
        return steps;
    }
    
    private List<Map<String, String>> collectFields(Scanner scanner, String type) {
        List<Map<String, String>> fields = new ArrayList<>();
        
        System.out.println("Define fields for " + type + " type (enter empty field name to continue):");
        
        while (true) {
            System.out.print("Field name (or Enter to finish " + type + " fields): ");
            String fieldName = scanner.nextLine().trim();
            
            if (fieldName.isEmpty()) {
                break;
            }
            
            System.out.println("Available types: string, int, long, double, boolean, uuid, bigdecimal, currency, path");
            System.out.print("Field type for '" + fieldName + "': ");
            String fieldType = scanner.nextLine().trim();
            
            if (fieldType.isEmpty()) {
                fieldType = "string"; // default to string
            }
            
            Map<String, String> field = new HashMap<>();
            field.put("name", fieldName);
            field.put("type", mapFieldType(fieldType));
            field.put("protoType", mapToProtoType(fieldType));
            fields.add(field);
        }
        
        return fields;
    }
    
    private String mapFieldType(String inputType) {
        switch (inputType.toLowerCase()) {
            case "string": return "String";
            case "int": return "Integer";
            case "long": return "Long";
            case "double": return "Double";
            case "boolean": return "Boolean";
            case "uuid": return "UUID";
            case "bigdecimal": return "BigDecimal";
            case "currency": return "Currency";
            case "path": return "Path";
            default: return "String"; // default to String
        }
    }
    
    private String mapToProtoType(String inputType) {
        switch (inputType.toLowerCase()) {
            case "string": return "string";
            case "int": return "int32";
            case "long": return "int64";
            case "double": return "double";
            case "boolean": return "bool";
            case "uuid": return "string";
            case "bigdecimal": return "string";
            case "currency": return "string";
            case "path": return "string";
            default: return "string"; // default to string
        }
    }
    
    private String toCamelCase(String input) {
        String[] parts = input.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() > 0) {
                if (i == 0) {
                    result.append(Character.toLowerCase(part.charAt(0)));
                } else {
                    result.append(Character.toUpperCase(part.charAt(0)));
                }
                result.append(part.substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TemplateGeneratorCli()).execute(args);
        System.exit(exitCode);
    }
}