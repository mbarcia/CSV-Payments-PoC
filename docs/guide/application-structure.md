# Application Structure

This guide explains how to structure applications using the Pipeline Framework, following the patterns demonstrated in the CSV Payments reference implementation.

## Overview

Applications built with the Pipeline Framework follow a modular architecture with clear separation of concerns. The framework promotes a clean division between:

1. **Orchestrator Service**: Coordinates the overall pipeline execution
2. **Backend Services**: Implement individual pipeline steps
3. **Common Module**: Shared domain objects, DTOs, and mappers
4. **Framework**: Provides the pipeline infrastructure

## Project Structure

A typical pipeline application follows this structure:

```text
my-pipeline-application/
├── pom.xml                           # Parent POM
├── common/                           # Shared components
│   ├── pom.xml
│   └── src/
│       └── main/java/
│           └── com/example/app/common/
│               ├── domain/           # Domain entities
│               ├── dto/              # Data transfer objects
│               └── mapper/           # Shared mappers
├── orchestrator-svc/                 # Pipeline orchestrator
│   ├── pom.xml
│   └── src/
│       └── main/java/
│           └── com/example/app/orchestrator/
│               ├── service/         # Pipeline execution service
│               └── CsvPaymentsApplication.java
├── step-one-svc/                     # First pipeline step
│   ├── pom.xml
│   └── src/
│       └── main/java/
│           └── com/example/app/stepone/
│               ├── service/         # Step implementation
│               └── mapper/          # Step-specific mappers
├── step-two-svc/                     # Second pipeline step
│   ├── pom.xml
│   └── src/
│       └── main/java/
│           └── com/example/app/steptwo/
│               ├── service/        # Step implementation
│               └── mapper/          # Step-specific mappers
└── pipeline-framework/              # Framework modules
    ├── runtime/
    ├── deployment/
    └── pom.xml
```

## Common Module Structure

The common module contains shared components used across all services:

### Domain Entities
Domain entities represent the core business concepts:

```java
// common/src/main/java/com/example/app/common/domain/PaymentRecord.java
public class PaymentRecord {
    private UUID id;
    private String csvId;
    private String recipient;
    private BigDecimal amount;
    private Currency currency;
    // constructors, getters, setters...
}
```

### DTOs
Data Transfer Objects are used for inter-service communication:

```java
// common/src/main/java/com/example/app/common/dto/PaymentRecordDto.java
public class PaymentRecordDto {
    private UUID id;
    private String csvId;
    private String recipient;
    private BigDecimal amount;
    private Currency currency;
    // constructors, getters, setters...
}
```

### Shared Mappers
Mappers that are used across multiple services should be in the common module:

```java
// common/src/main/java/com/example/app/common/mapper/PaymentRecordMapper.java
@Mapper(componentModel = "cdi")
public interface PaymentRecordMapper {
    PaymentRecordMapper INSTANCE = Mappers.getMapper(PaymentRecordMapper.class);
    
    PaymentRecord fromDto(PaymentRecordDto dto);
    PaymentRecordDto toDto(PaymentRecord entity);
    
    // gRPC conversions if needed
    PaymentRecordGrpc fromGrpc(PaymentRecordGrpcOuterClass.PaymentRecord grpc);
    PaymentRecordGrpcOuterClass.PaymentRecord toGrpc(PaymentRecord entity);
}
```

## Backend Service Structure

Each backend service implements a specific pipeline step:

### Service Implementation
Annotate your service class with `@PipelineStep`:

```java
// step-one-svc/src/main/java/com/example/app/stepone/service/ProcessPaymentStep.java
@PipelineStep(
    order = 1,
    inputType = PaymentRecord.class,
    outputType = PaymentStatus.class,
    stepType = StepOneToOne.class,
    backendType = GenericGrpcReactiveServiceAdapter.class,
    grpcStub = MutinyProcessPaymentServiceGrpc.MutinyProcessPaymentServiceStub.class,
    grpcImpl = MutinyProcessPaymentServiceGrpc.ProcessPaymentServiceImplBase.class,
    inboundMapper = PaymentRecordInboundMapper.class,
    outboundMapper = PaymentStatusOutboundMapper.class,
    grpcClient = "process-payment",
    autoPersist = true,
    debug = true
)
@ApplicationScoped
public class ProcessPaymentStep implements StepOneToOne<PaymentRecord, PaymentStatus> {
    
    @Override
    public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
        // Implementation here
        return Uni.createFrom().item(/* processed payment status */);
    }
}
```

### Step-Specific Mappers
Mappers that are specific to a step should be in that service's module:

```java
// step-one-svc/src/main/java/com/example/app/stepone/mapper/PaymentRecordInboundMapper.java
@ApplicationScoped
public class PaymentRecordInboundMapper implements InboundMapper<PaymentRecordGrpc, PaymentRecord> {
    
    @Override
    public PaymentRecord fromGrpc(PaymentRecordGrpc grpc) {
        // Convert gRPC to domain
        return new PaymentRecord(/* ... */);
    }
}
```

## Orchestrator Service Structure

The orchestrator service coordinates the pipeline execution:

### Pipeline Application
The orchestrator extends the framework's `PipelineApplication`:

```java
// orchestrator-svc/src/main/java/com/example/app/orchestrator/CsvPaymentsApplication.java
@QuarkusMain
@CommandLine.Command(
    name = "csv-payments",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Process CSV payment files")
public class CsvPaymentsApplication extends PipelineApplication implements Runnable, QuarkusApplication {
    
    @CommandLine.Option(
        names = {"-c", "--csv-folder"},
        description = "The folder path containing CSV payment files",
        defaultValue = "${env:CSV_FOLDER_PATH:-csv/}")
    String csvFolder;
    
    @Override
    public void run() {
        processPipeline(csvFolder);
    }
    
    @Override
    public void processPipeline(String input) {
        // This method is called by the generated pipeline application
        // The framework handles the actual pipeline execution
    }
}
```

### Pipeline Execution Service
Handles the orchestration logic:

```java
// orchestrator-svc/src/main/java/com/example/app/orchestrator/service/ProcessFolderService.java
@ApplicationScoped
public class ProcessFolderService {
    
    public Stream<CsvPaymentsInputFile> process(String csvFolderPath) {
        // Logic to read CSV files and convert to domain objects
        return Stream.of(/* ... */);
    }
}
```

## Dependency Management

### Parent POM
The parent POM defines common properties and manages dependencies:

```xml
<!-- pom.xml -->
<project>
    <groupId>com.example</groupId>
    <artifactId>my-pipeline-application-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>common</module>
        <module>pipeline-framework/runtime</module>
        <module>pipeline-framework/deployment</module>
        <module>step-one-svc</module>
        <module>step-two-svc</module>
        <module>orchestrator-svc</module>
    </modules>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>pipeline-framework-runtime</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### Service POMs
Services declare dependencies on the common module and framework:

```xml
<!-- step-one-svc/pom.xml -->
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-pipeline-application-parent</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>step-one-svc</artifactId>
    
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>pipeline-framework-runtime</artifactId>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>pipeline-framework-deployment</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

## Configuration

### Application Properties
Configure pipeline behavior through application properties:

```properties
# application.properties
# Pipeline configuration
pipeline.retry-limit=3
pipeline.debug=false
pipeline.auto-persist=true

# gRPC clients
quarkus.grpc.clients.process-payment.host=localhost
quarkus.grpc.clients.process-payment.port=8080
```

### Environment-Specific Profiles
Use Quarkus profiles for different environments:

```properties
# application-dev.properties
pipeline.debug=true
pipeline.retry-limit=1

# application-prod.properties
pipeline.retry-limit=5
pipeline.retry-wait=1S
```

## Best Practices

### Modularity
- Keep services focused on a single responsibility
- Share only what's necessary through the common module
- Minimize circular dependencies

### Testing
- Test steps in isolation
- Use the framework's testing utilities
- Validate mapper correctness
- Test error scenarios

### Observability
- Enable framework's built-in metrics and tracing
- Add meaningful log statements
- Monitor pipeline performance

### Deployment
- Package services as independent deployable units
- Use containerization for consistent deployment
- Configure health checks and readiness probes