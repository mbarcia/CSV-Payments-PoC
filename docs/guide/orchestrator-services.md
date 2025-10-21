# Creating Orchestrator Services

This guide explains how orchestrator services work in The Pipeline Framework and how they are automatically generated when you use the template generator to create pipeline applications.

<Callout type="tip" title="Visual Orchestrator Configuration">
Use the Canvas designer at <a href="https://app.pipelineframework.org" target="_blank">https://app.pipelineframework.org</a> to visually configure your orchestrator services. The Canvas allows you to define the complete pipeline flow, including input sources, step connections, and output handlers, without writing complex orchestration code.
</Callout>

## Overview

Orchestrator services are responsible for:
1. Initiating the pipeline execution
2. Providing input data to the pipeline
3. Coordinating the flow between pipeline steps
4. Handling the final output of the pipeline

When you use the template generator to create a pipeline application, it automatically generates a complete orchestrator service with:
- A CLI application class that extends `PipelineApplication`
- Proper configuration in `application.properties`
- Docker configuration for containerized deployment
- Integration with the framework's pipeline execution engine

The Pipeline Framework automatically generates the core pipeline execution logic when backend services are annotated with `@PipelineStep`, leaving orchestrator services to focus on input provisioning and output handling.

## Generated Orchestrator Service Structure

When the template generator creates an application, it generates an orchestrator service with the following structure:

```text
orchestrator-svc/
├── pom.xml                           # Service POM with framework dependencies
├── src/main/java/
│   └── com/example/app/orchestrator/
│       └── OrchestratorApplication.java  # Main CLI application class with input provisioning stub
└── src/main/resources/
    └── application.properties            # Service configuration
```

### OrchestratorApplication.java

The generated orchestrator application extends `PipelineApplication` and includes a `getInputMulti()` method stub that needs to be implemented by the user. This method is responsible for provisioning input data to the pipeline:

```java
// orchestrator-svc/src/main/java/com/example/app/orchestrator/OrchestratorApplication.java
@Command(name = "orchestrator", mixinStandardHelpOptions = true, version = "1.0.0",
         description = "Sample Pipeline App Orchestrator Service")
@Dependent
public class OrchestratorApplication implements QuarkusApplication {

    @Option(names = {"-i", "--input"}, description = "Input value for the pipeline", required = true)
    String input;

    @Inject
    PipelineExecutionService pipelineExecutionService;

    public static void main(String[] args) {
        io.quarkus.runtime.Quarkus.run(OrchestratorApplication.class, args);
    }

    @Override
    public int run(String... args) {
        CommandLine cmd = new CommandLine(this);
        cmd.parseArgs(args);

        if (input != null) {
            executePipelineWithInput(input);
            return 0; // Success exit code
        } else {
            System.err.println("Input parameter is required");
            return 1; // Error exit code
        }
    }

    // Execute the pipeline when arguments are properly parsed
    private void executePipelineWithInput(String input) {
        Multi<{{firstInputTypeName}}> inputMulti = getInputMulti(input);

        // Execute the pipeline with the processed input using injected service
        pipelineExecutionService.executePipeline(inputMulti)
            .collect().asList()
            .await().indefinitely();

        System.out.println("Pipeline execution completed");
    }

    // This method needs to be implemented by the user after template generation
    // based on their specific input type and requirements
    private static Multi<{{firstInputTypeName}}> getInputMulti(String input) {
        // TODO: User needs to implement this method after template generation
        // Create and return appropriate Multi based on the input and first step requirements
        // For example:
        // {{firstInputTypeName}} inputItem = new {{firstInputTypeName}}();
        // inputItem.setField(input);
        // return Multi.createFrom().item(inputItem);
        
        throw new UnsupportedOperationException("Method getInputMulti needs to be implemented by user after template generation");
    }
}
```

**Important**: After template generation, you must implement the `getInputMulti()` method to define how your application provisions input data to the pipeline. This method should parse your input parameters and create the appropriate `Multi` stream of objects required by your first pipeline step.

### Application Properties

The generated orchestrator includes comprehensive configuration:

```properties
# orchestrator-svc/src/main/resources/application.properties
quarkus.package.main-class=com.example.sample.orchestrator.OrchestratorApplication

# Pipeline Configuration
pipeline.runtime.retry-limit=10
pipeline.runtime.retry-wait-ms=500
pipeline.runtime.debug=false
pipeline.runtime.recover-on-failure=false
pipeline.runtime.run-with-virtual-threads=false
pipeline.runtime.auto-persist=true
pipeline.runtime.max-backoff=30000
pipeline.runtime.jitter=false

# gRPC client configurations for each service
quarkus.grpc.clients.processCustomer.host=process-customer-svc
quarkus.grpc.clients.processCustomer.port=8444
quarkus.grpc.clients.processCustomer.plain-text=false
quarkus.grpc.clients.processCustomer.use-quarkus-grpc-client=true
quarkus.grpc.clients.processCustomer.tls.enabled=true

# ... additional service configurations
```

## Customizing Generated Orchestrator Services

While the template generator creates a complete orchestrator service, you can customize it for your specific needs:

### 1. Implement Input Provisioning

The template generates a `getInputMulti()` method stub that you must implement to provision inputs to your pipeline. This method converts your command-line input parameters into the appropriate `Multi` stream:

```java
// orchestrator-svc/src/main/java/com/example/app/orchestrator/OrchestratorApplication.java
@Dependent
public class OrchestratorApplication implements QuarkusApplication {

    // ... other code ...

    // After template generation, implement this method to provision inputs:
    private static Multi<{{firstInputTypeName}}> getInputMulti(String input) {
        // Example implementation:
        // 1. Parse input string for file paths, database queries, or other input sources
        // 2. Convert to the appropriate domain objects for your pipeline
        // 3. Return a Multi stream of these objects
        
        // For example, if your input is a CSV file path:
        List<{{firstInputTypeName}}> inputList = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(input))) {
            // Process CSV and create {{firstInputTypeName}} objects
            // inputList.add(...);
        } catch (IOException e) {
            throw new RuntimeException("Error reading input file", e);
        }
        
        return Multi.createFrom().iterable(inputList);
    }
}
```

### 2. Customize OrchestratorApplication

Add custom logic to the generated `OrchestratorApplication`. You can modify command-line options or add additional processing:

```java
@Command(name = "orchestrator", mixinStandardHelpOptions = true, version = "1.0.0",
         description = "My Custom Pipeline App Orchestrator Service")
@Dependent
public class OrchestratorApplication implements QuarkusApplication {

    @Option(names = {"-i", "--input"}, description = "Input value for the pipeline", required = true)
    String input;
    
    // Add custom command-line options
    @Option(names = {"--custom-option"},
            description = "A custom option for this orchestrator")
    String customOption;

    @Inject
    PipelineExecutionService pipelineExecutionService;

    public static void main(String[] args) {
        io.quarkus.runtime.Quarkus.run(OrchestratorApplication.class, args);
    }

    @Override
    public int run(String... args) {
        CommandLine cmd = new CommandLine(this);
        cmd.parseArgs(args);

        if (input != null) {
            executePipelineWithInput(input);
            return 0; // Success exit code
        } else {
            System.err.println("Input parameter is required");
            return 1; // Error exit code
        }
    }

    // Execute the pipeline when arguments are properly parsed
    private void executePipelineWithInput(String input) {
        Multi<{{firstInputTypeName}}> inputMulti = // call getInputMulti(input) or custom input processing;

        // Execute the pipeline with the processed input using injected service
        pipelineExecutionService.executePipeline(inputMulti)
            .collect().asList()
            .await().indefinitely();

        System.out.println("Pipeline execution completed");
    }

    // Implement this method to provision inputs:
    private static Multi<{{firstInputTypeName}}> getInputMulti(String input) {
        // Your implementation to convert input to Multi<{{firstInputTypeName}}>
        throw new UnsupportedOperationException("Method needs to be implemented");
    }
}
```

## Manual Orchestrator Service Creation

<Callout type="info" title="Advanced Users Only">
The following section describes how to manually create orchestrator services for advanced users who need fine-grained control over their implementations. For most use cases, we recommend using the template generator.
</Callout>

### 1. Implement Orchestrator Application

Create your orchestrator service by implementing the QuarkusApplication interface with proper integration to the pipeline execution framework:

```java
// orchestrator-svc/src/main/java/com/example/app/orchestrator/CsvPaymentsApplication.java
@Command(name = "csv-payments", mixinStandardHelpOptions = true, version = "1.0.0",
         description = "Process CSV payment files")
@Dependent
public class CsvPaymentsApplication implements QuarkusApplication {

    @Option(names = {"-f", "--file"}, description = "Input CSV file path", required = true)
    String inputFile;

    @Inject
    PipelineExecutionService pipelineExecutionService;

    public static void main(String[] args) {
        io.quarkus.runtime.Quarkus.run(CsvPaymentsApplication.class, args);
    }

    @Override
    public int run(String... args) {
        CommandLine cmd = new CommandLine(this);
        cmd.parseArgs(args);

        if (inputFile != null) {
            executePipelineWithInput(inputFile);
            return 0; // Success exit code
        } else {
            System.err.println("Input file parameter is required");
            return 1; // Error exit code
        }
    }

    // Execute the pipeline when arguments are properly parsed
    private void executePipelineWithInput(String inputFile) {
        Multi<{{firstInputTypeName}}> inputMulti = getInputMulti(inputFile);

        // Execute the pipeline with the processed input using injected service
        pipelineExecutionService.executePipeline(inputMulti)
            .collect().asList()
            .await().indefinitely();

        System.out.println("Pipeline execution completed");
    }

    // Implementation to convert your input into Multi stream
    private static Multi<{{firstInputTypeName}}> getInputMulti(String inputFile) {
        // TODO: Implement this method to read the input file and convert to Multi<{{firstInputTypeName}}>
        // For example, process CSV file and return Multi stream of {{firstInputTypeName}} objects
        throw new UnsupportedOperationException("Method getInputMulti needs to be implemented");
    }
}
```

[Rest of the original content...]