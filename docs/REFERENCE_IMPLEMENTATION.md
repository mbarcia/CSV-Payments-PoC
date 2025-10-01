# The Pipeline Framework: Reference Implementation

## Overview

This document provides a comprehensive reference implementation of The Pipeline Framework, demonstrating how to create pipeline applications using both YAML configuration files and the visual Canvas tool.

## Quick Start with YAML Configuration

### 1. Generate Sample Configuration

First, generate a sample configuration file:

```bash
java -jar template-generator-1.0.0.jar --generate-config
```

This creates `sample-pipeline-config.yaml` with a complete example configuration.

### 2. Customize Configuration

Edit the generated YAML file to define your pipeline:

```yaml
---
appName: "My Pipeline App"
basePackage: "com.example.mypipeline"
steps:
- name: "Process Customer"
  cardinality: "ONE_TO_ONE"
  inputTypeName: "CustomerInput"
  inputFields:
  - name: "id"
    type: "UUID"
    protoType: "string"
  - name: "name" 
    type: "String"
    protoType: "string"
  - name: "email"
    type: "String"
    protoType: "string"
  outputTypeName: "CustomerOutput"
  outputFields:
  - name: "id"
    type: "UUID"
    protoType: "string"
  - name: "name"
    type: "String"
    protoType: "string"
  - name: "status"
    type: "String"
    protoType: "string"
  - name: "processedAt"
    type: "String"
    protoType: "string"
- name: "Validate Order"
  cardinality: "ONE_TO_ONE"
  inputTypeName: "CustomerOutput"  # Automatically uses output of previous step
  inputFields:  # Automatically inherits fields from CustomerOutput
  - name: "id"
    type: "UUID"
    protoType: "string"
  - name: "name"
    type: "String"
    protoType: "string"
  - name: "status"
    type: "String"
    protoType: "string"
  - name: "processedAt"
    type: "String"
    protoType: "string"
  outputTypeName: "ValidationOutput"
  outputFields:
  - name: "id"
    type: "UUID"
    protoType: "string"
  - name: "isValid"
    type: "Boolean"
    protoType: "bool"
```

### 3. Generate Application

Generate the complete application from your configuration:

```bash
java -jar template-generator-1.0.0.jar --config my-pipeline-config.yaml --output ./my-pipeline-app
```

## Pipeline Step Types and Cardinalities

### ONE_TO_ONE (1-1)
Transforms single input to single output:

```java
@PipelineStep(
    order = 1,
    inputType = CustomerInput.class,
    outputType = CustomerOutput.class,
    stepType = StepOneToOne.class,
    backendType = GenericGrpcReactiveServiceAdapter.class
)
public class ProcessCustomerStep implements StepOneToOne<CustomerInput, CustomerOutput> {
    @Override
    public Uni<CustomerOutput> apply(Uni<CustomerInput> input) {
        // Implementation here
    }
}
```

### EXPANSION (1-Many)
Transforms single input to multiple outputs:

```java
@PipelineStep(
    order = 2,
    inputType = CustomerOutput.class,
    outputType = OrderInput.class,
    stepType = StepOneToMany.class,
    backendType = GenericGrpcReactiveServiceAdapter.class
)
public class GenerateOrdersStep implements StepOneToMany<CustomerOutput, OrderInput> {
    @Override
    public Multi<OrderInput> apply(Uni<CustomerOutput> input) {
        // Implementation here
    }
}
```

### REDUCTION (Many-1)
Aggregates multiple inputs to single output:

```java
@PipelineStep(
    order = 3,
    inputType = OrderInput.class,
    outputType = SummaryOutput.class,
    stepType = StepManyToOne.class,
    backendType = GenericGrpcReactiveServiceAdapter.class
)
public class AggregateOrdersStep implements StepManyToOne<OrderInput, SummaryOutput> {
    @Override
    public Uni<SummaryOutput> apply(Multi<OrderInput> input) {
        // Implementation here
    }
}
```

### SIDE_EFFECT (1-1 with same input/output)
Performs side effects without changing data:

```java
@PipelineStep(
    order = 4,
    inputType = SummaryOutput.class,
    outputType = SummaryOutput.class,  # Same as input type
    stepType = StepOneToOne.class,
    backendType = GenericGrpcReactiveServiceAdapter.class
)
public class LogSummaryStep implements StepOneToOne<SummaryOutput, SummaryOutput> {
    @Override
    public Uni<SummaryOutput> apply(Uni<SummaryOutput> input) {
        // Side effect implementation, returns same input
    }
}
```

## Quick Start with Visual Canvas

### 1. Visit the Canvas Designer
Go to https://app.pipelineframework.org to use the visual designer.

### 2. Create Steps Visually
- Drag and drop new steps onto the canvas
- Select cardinality for each step
- Define input/output types and fields
- Connect steps to establish pipeline flow

### 3. Download Configuration
- Generate YAML configuration from the visual design
- Download the configuration file
- Use it with the template generator:

```bash
java -jar template-generator-1.0.0.jar -c downloaded-config.yaml -o ./my-pipeline-app
```

## Generated Project Structure

The template generator creates a complete Maven multi-module project:

```
my-pipeline-app/
├── pom.xml                       # Parent POM with all modules
├── common/                       # Shared components
│   ├── pom.xml
│   ├── src/main/proto/           # gRPC proto definitions
│   ├── src/main/java/com/example/mypipeline/common/domain/    # Entity classes
│   ├── src/main/java/com/example/mypipeline/common/dto/       # DTO classes
│   └── src/main/java/com/example/mypipeline/common/mapper/    # Mapper classes
├── process-customer-svc/         # First pipeline step service
│   ├── pom.xml
│   ├── src/main/java/...        # Service implementation
│   └── Dockerfile
├── validate-order-svc/           # Second pipeline step service
│   ├── pom.xml
│   ├── src/main/java/...        # Service implementation
│   └── Dockerfile
├── orchestrator-svc/             # Orchestrator service
│   ├── pom.xml
│   └── Dockerfile
├── docker-compose.yml            # Service orchestration
├── up-docker.sh                  # Docker startup script
├── down-docker.sh                # Docker shutdown script
├── otel-collector-config.yaml    # OpenTelemetry configuration
├── prometheus.yml                # Prometheus configuration
├── grafana-datasources.yaml      # Grafana data sources
├── grafana-dashboards.yaml       # Grafana dashboards
├── tempo.yaml                    # Tempo configuration
├── mvnw                          # Maven wrapper (Unix)
├── mvnw.cmd                      # Maven wrapper (Windows)
├── .mvn/wrapper/                 # Maven wrapper files
└── README.md                     # Project documentation
```

## Implementation Workflow

### 1. Define Pipeline Configuration
- Use YAML file or Canvas designer to define steps
- Specify cardinalities and data types
- Define field mappings for each step

### 2. Generate Application
- Run template generator to create full project
- Generated code includes all necessary components
- Observability stack is included by default

### 3. Implement Business Logic
- Fill in the `apply()` methods in generated service classes
- Use reactive patterns with Uni/Multi as appropriate
- Add domain-specific logic and validations

### 4. Test and Deploy
- Use generated Docker Compose for local testing
- Deploy individual services as needed
- Monitor with integrated observability tools

## Field Type Mapping

The framework supports a rich Java-centered type system with automatic protobuf conversion:

| Java Type | Protobuf Equivalent | Use Case |
|-----------|-------------------|----------|
| String | string | Text fields |
| Integer | int32 | Small integer values |
| Long | int64 | Large integer values |
| Double | double | Floating-point numbers |
| Boolean | bool | True/false values |
| UUID | string | Unique identifiers |
| BigDecimal | string | Precise decimal numbers |
| Currency | string | Currency codes |
| Path | string | File system paths |
| `List<String>` | repeated string | String collections |
| LocalDateTime | string | Date and time without zone |
| LocalDate | string | Date only |
| OffsetDateTime | string | Date and time with offset |
| ZonedDateTime | string | Date and time with zone |
| Instant | int64 | Timestamp (epoch milliseconds) |
| Duration | int64 | Time duration (nanoseconds) |
| Period | string | Date-based period |
| URI | string | Uniform Resource Identifier |
| URL | string | Uniform Resource Locator |
| File | string | File system objects |
| BigInteger | string | Large integers |
| AtomicInteger | int32 | Thread-safe counters |
| AtomicLong | int64 | Thread-safe large counters |

### Automatic Conversion
- MapStruct handles automatic conversion between Java and protobuf types for built-in types
- Built-in conversions: primitives, wrappers, UUID, BigDecimal/BigInteger, Java 8 time types, URI/URL/File/Path
- Custom converters in `CommonConverters` class manage specialized mappings: Currency, AtomicInteger, AtomicLong, `List<String>`
- Null-safe conversions for all supported types

## Inter-Step Dependencies

The framework automatically manages type dependencies between steps:

1. **Automatic Imports**: Generated proto files import types from previous steps
2. **Type Propagation**: Output types of one step become input types of the next
3. **Validation**: Configuration validation ensures type consistency
4. **Synchronization**: Field synchronization across connected steps

## Best Practices

1. **Start Small**: Begin with simple 1-1 steps to understand the flow
2. **Use Canvas**: For complex pipelines, use the visual designer first
3. **Test Incrementally**: Implement and test each step separately
4. **Monitor**: Use the integrated observability tools to monitor performance
5. **Document**: Add documentation for complex business logic