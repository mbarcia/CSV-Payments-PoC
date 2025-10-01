# The Pipeline Framework: Comprehensive Overview

## Introduction

The Pipeline Framework is a sophisticated system for building reactive pipeline processing applications. It provides both programmatic and visual development approaches, centered around the concept of interconnected pipeline steps that process data reactively.

## Original Specification vs. Current Implementation

### Original Template Generation Spec (TEMPLATE_GENERATION.md)

The original specification proposed a wizard-like CLI tool that would ask users questions to generate pipeline applications:
- Application name
- Base package name
- Step definitions (name, cardinality, input/output types, fields)
- Field types based on protobuf

The spec called for generating:
- Parent POM with modules
- Docker Compose orchestration
- Utility scripts
- Common module with proto definitions, entities, DTOs and mappers
- Microservice modules with annotated services
- Orchestrator module

### Current Implementation

The framework has evolved significantly beyond the original spec to include:

#### 1. Dual Development Approaches
- **Programmatic**: Annotation-driven development with `@PipelineStep`
- **Visual**: Web-based Canvas designer at https://app.pipelineframework.org

#### 2. Annotation-Based Infrastructure
The core innovation is the `@PipelineStep` annotation processor that automatically generates:
- gRPC and REST adapters
- Pipeline orchestration
- Service integration
- Build-time infrastructure

#### 3. Visual Canvas Designer
- Web-based visual interface for pipeline design
- Interactive workflow creation
- YAML configuration generation
- Real-time field synchronization across connected steps

#### 4. Enhanced Architecture Patterns
- Reactive programming with Mutiny
- Virtual threads for high concurrency
- Backpressure handling
- Side-effect steps
- Multiple step cardinalities (1-1, 1-many, many-1, many-many, side-effect)

#### 5. Template Generator (Command Line Tool)
- **Java DTO-Centered Types**: Rich Java type system with automatic protobuf conversion instead of protobuf-limited types
- **Interactive Mode**: Step-by-step CLI wizard to collect pipeline specifications with comprehensive type options
- **YAML File Mode**: Generate applications from predefined YAML configuration files
- **YAML Generation Mode**: Create sample configuration files for reference
- **Mustache Templating**: Code generation using Mustache templates for all components
- **Complete Project Generation**: Creates full Maven multi-module projects with all necessary files
- **Automatic Import Management**: Intelligent import generation based on used Java types
- **MapStruct Integration**: Automatic conversion between Java DTOs and protobuf with intelligent built-in and custom converters

#### 6. Java Type System
- **Expanded Type Support**: String, Integer, Long, Double, Boolean, UUID, BigDecimal, Currency, Path, `List<String>`, LocalDateTime, LocalDate, OffsetDateTime, ZonedDateTime, Instant, Duration, Period, URI, URL, File, BigInteger, AtomicInteger, AtomicLong
- **Automatic Protobuf Mapping**: Complex Java types map to appropriate protobuf equivalents
- **Built-in Conversions**: MapStruct automatically handles primitive types, wrappers, UUID, BigDecimal/BigInteger, Java 8 time types, and URI/URL/File/Path
- **Custom Converters**: CommonConverters class provides specialized conversion functions for Currency, AtomicInteger, AtomicLong, `List<String>`
- **Date/Time Support**: Full Java 8 time API integration with ISO format parsing
- **Collection Support**: `List<String>` maps to repeated string in protobuf
- **Atomic Types**: Thread-safe atomic types with protobuf conversion

#### 7. Design Philosophy Differences
- **New Projects**: Follow MapStruct best practices with minimal custom converters (template generator approach)
- **Legacy Systems**: Maintain existing custom converters with specific business logic (CSV Payments approach)
- **Migration Path**: New projects leverage MapStruct's built-ins, existing systems can refactor gradually

The template generator implements the original spec with significant enhancements:
- Supports multiple cardinalities: ONE_TO_ONE, EXPANSION, REDUCTION, SIDE_EFFECT
- Field type mapping between Java, Protobuf, and JSON
- Automatic inter-step type dependency management
- Complete project scaffolding with observability stack
- Maven wrapper integration
- Docker Compose orchestration with service networking
- Utility scripts for local and Docker deployment
- Comprehensive schema validation for YAML configs

### Template Generator Architecture

#### CLI Component (`TemplateGeneratorCli.java`)
- Uses Picocli for command-line parsing
- Supports multiple execution modes:
  - Interactive mode (`-i` or default)
  - Configuration file mode (`-c config.yaml`)
  - Sample config generation (`-g`)
- Field type mapping system with validation
- Automatic inter-step type dependency tracking

#### Template Engine (`MustacheTemplateEngine.java`)
- Generates complete Maven multi-module project
- Creates all necessary modules: parent, common, steps, orchestrator
- Generates Java entities, DTOs, and mappers
- Creates gRPC proto definitions with proper imports
- Generates microservice implementations with `@PipelineStep` annotations
- Creates Docker configuration and deployment scripts
- Generates observability stack (Prometheus, Grafana, Tempo, Loki, OpenTelemetry)

#### Configuration Schema
- JSON Schema for YAML configuration validation
- Strict type checking for Java and Protobuf types
- Cardinality validation and dependency management
- Required field enforcement

## Framework Architecture

### Core Components

#### 1. Annotation Processor (`@PipelineStep`)
```java
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
```

#### 2. Reactive Step Interfaces
- `StepOneToOne<I, O>`: Single input to single output
- `StepOneToMany<I, O>`: Single input to multiple outputs
- `StepManyToOne<I, O>`: Multiple inputs to single output
- `StepManyToMany<I, O>`: Multiple inputs to multiple outputs
- `StepSideEffect<I, O>`: Side-effect processing (input=output)

#### 3. Type System
- Protobuf scalar types support
- Custom message types from previous steps
- Comprehensive validation for type dependencies
- Bidirectional field synchronization

### Project Structure

#### Template Generator (`template-generator/`)
- Interactive CLI for pipeline generation
- YAML configuration support
- Mustache templating engine
- Complete project scaffolding

#### Pipeline Core Library (`pipeline/`)
- Reusable pipeline components
- Reactive service interfaces
- gRPC adapters and infrastructure
- Annotation processing

#### Web UI (`web-ui/`)
- Visual canvas interface
- Interactive pipeline designer
- Real-time YAML generation
- Field synchronization between steps
- Type validation and error handling

#### Observability Stack
- OpenTelemetry integration
- Prometheus metrics
- Grafana dashboards
- Tempo distributed tracing
- Loki logging

## Canvas Designer Features

The Canvas (https://app.pipelineframework.org) provides:

### Visual Pipeline Design
- Interactive canvas with step placement
- Different arrow types for cardinalities
- Clickable steps showing combined forms
- Field synchronization between connected steps
- Type validation and dependency management

### Real-time Configuration
- Live YAML generation from visual changes
- Downloadable configuration files
- Upload support for existing configurations
- Two-column layout for input/output comparison
- Animated interface elements for better UX

### Advanced Features
- Side-effect step validation
- Custom message type support
- Field type validation (scalar or custom)
- Confirmation dialogs for destructive operations
- Responsive design for different screen sizes

## Key Innovations

### 1. Automatic Adapter Generation
The annotation processor eliminates the disconnect between step configuration and adapter configuration, automatically generating gRPC and REST adapters based on `@PipelineStep` annotations.

### 2. Visual + Programmatic Duality
Users can choose between visual design (Canvas) and programmatic development, with both approaches producing equivalent configurations.

### 3. Reactive by Default
Built on Mutiny for non-blocking, high-performance reactive processing with virtual thread support.

### 4. Type Safety & Validation
Sophisticated type system that validates dependencies between steps and ensures type consistency across the pipeline.

### 5. Comprehensive Observability
Integrated OpenTelemetry, metrics, tracing, and logging stack for monitoring pipeline performance.

## DevOps Integration

### Build Tools
- Maven wrapper for consistent builds
- Annotation processing at build time
- Docker containerization
- Docker Compose orchestration

### Deployment Options
- Individual microservice deployment
- Container orchestration
- Cloud deployment capabilities
- Static web UI for Canvas tool

### Testing & Quality
- Automated test generation
- Integration testing support
- Performance monitoring
- Error handling with DLQ support

## Comparison with Original Spec

| Feature | Original Spec | Current Implementation | Status |
|---------|---------------|----------------------|---------|
| CLI Wizard | ✅ Basic wizard | ✅ Enhanced wizard + visual Canvas | Exceeded |
| Project Generation | ✅ File-based scaffolding | ✅ Dynamic generation with validation | Exceeded |
| Microservices | ✅ Basic structure | ✅ Reactive with gRPC/REST | Exceeded |
| Configuration | ✅ Manual definition | ✅ Visual + YAML + Annotation | Exceeded |
| Type System | ❌ Basic types only | ✅ Protobuf + custom message types | Added |
| Validation | ❌ Basic validation | ✅ Comprehensive type validation | Added |
| Visual Design | ❌ Not mentioned | ✅ Canvas designer | Added |
| Reactive Processing | ❌ Not specified | ✅ Mutiny-based reactive | Added |
| Observability | ❌ Not mentioned | ✅ Full stack integration | Added |
| Deployment | ❌ Basic Docker | ✅ Orchestration & monitoring | Enhanced |

## Use Cases

The framework is suitable for:
- Data processing pipelines
- ETL workflows
- Event-driven architectures
- Microservice orchestration
- Real-time data transformation
- Batch processing systems

## Conclusion

The Pipeline Framework has evolved significantly from the original template generation spec into a comprehensive platform supporting both visual and programmatic pipeline development. The addition of the Canvas designer, sophisticated type system, annotation processor, and reactive architecture represents a substantial advancement over the original concept.