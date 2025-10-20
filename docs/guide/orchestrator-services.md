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
│       ├── OrchestratorApplication.java  # Main CLI application class
│       └── service/
│           └── ProcessFolderService.java   # Input provisioning service
└── src/main/resources/
    └── application.properties            # Service configuration
```

### OrchestratorApplication.java

The generated orchestrator application extends `PipelineApplication` and includes:

```java
// orchestrator-svc/src/main/java/com/example/app/orchestrator/OrchestratorApplication.java
@QuarkusMain
@CommandLine.Command(
    name = "orchestrator",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Sample Pipeline App Orchestrator Service")
public class OrchestratorApplication extends PipelineApplication implements Runnable, QuarkusApplication {

    @Inject ProcessFolderService processFolderService;
    
    @CommandLine.Option(
        names = {"-c", "--csv-folder"},
        description = "The folder path containing CSV payment files",
        defaultValue = "${env:CSV_FOLDER_PATH:-csv/}")
    String csvFolder;

    public static void main(String[] args) {
        Quarkus.run(OrchestratorApplication.class, args);
    }

    @Override
    public int run(String... args) {
        return new CommandLine(this).execute(args);
    }

    @Override
    public void run() {
        processPipeline(csvFolder);
    }
}
```

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

### 1. Modify Input Provisioning

Customize the `ProcessFolderService` or create your own input provisioning service:

```java
// orchestrator-svc/src/main/java/com/example/app/orchestrator/service/ProcessFolderService.java
@ApplicationScoped
public class ProcessFolderService {

    @Inject
    HybridResourceLoader resourceLoader;

    public Stream<InputType> process(String inputPath) throws URISyntaxException {
        // Custom input processing logic
        URL resource = resourceLoader.getResource(inputPath);
        // ... process and return stream of input objects
    }
}
```

### 2. Extend PipelineApplication

Add custom logic to the generated `OrchestratorApplication`:

```java
@QuarkusMain
@CommandLine.Command(
    name = "orchestrator",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "My Custom Pipeline App Orchestrator Service")
public class OrchestratorApplication extends PipelineApplication implements Runnable, QuarkusApplication {
    
    // Add custom command-line options
    @CommandLine.Option(
        names = {"--custom-option"},
        description = "A custom option for this orchestrator")
    String customOption;
    
    // Add custom services
    @Inject
    CustomProcessingService customProcessingService;
    
    // Override methods to add custom behavior
    @Override
    public void run() {
        // Custom preprocessing
        customProcessingService.preProcess();
        
        // Call parent implementation
        super.run();
        
        // Custom postprocessing
        customProcessingService.postProcess();
    }
}
```

## Manual Orchestrator Service Creation

<Callout type="info" title="Advanced Users Only">
The following section describes how to manually create orchestrator services for advanced users who need fine-grained control over their implementations. For most use cases, we recommend using the template generator.
</Callout>

### 1. Extend PipelineApplication

Create your orchestrator service by extending the framework's `PipelineApplication`:

```java
// orchestrator-svc/src/main/java/com/example/app/orchestrator/CsvPaymentsApplication.java
@QuarkusMain
@CommandLine.Command(
    name = "csv-payments",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Process CSV payment files")
public class CsvPaymentsApplication extends PipelineApplication implements Runnable, QuarkusApplication {
    
    @Inject ProcessFolderService processFolderService;
    
    @CommandLine.Option(
        names = {"-c", "--csv-folder"},
        description = "The folder path containing CSV payment files",
        defaultValue = "${env:CSV_FOLDER_PATH:-csv/}")
    String csvFolder;

    public static void main(String[] args) {
        Quarkus.run(CsvPaymentsApplication.class, args);
    }

    @Override
    public int run(String... args) {
        return new CommandLine(this).execute(args);
    }

    @Override
    public void run() {
        processPipeline(csvFolder);
    }
}
```

[Rest of the original content...]