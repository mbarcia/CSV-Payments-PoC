# Getting Started

This guide walks you through the process of setting up and using the The Pipeline Framework in your project.

<Callout type="tip" title="Prerequisites">
Before you begin, ensure you have Java 21+ and Maven 3.8+ installed on your system.
</Callout>

<Callout type="tip" title="Visual Pipeline Designer">
In addition to programmatic configuration, you can use the visual Canvas designer at <a href="https://app.pipelineframework.org" target="_blank">https://app.pipelineframework.org</a> to create and configure your pipeline applications. The Canvas provides an intuitive interface for defining pipeline steps and their connections.
</Callout>

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- A Quarkus-based project (the framework is designed to work with Quarkus)

## Adding the Framework to Your Project

Add the following dependency to your `pom.xml`. Both runtime and deployment components are bundled in a single dependency:

```xml
<dependency>
  <groupId>io.github.mbarcia</groupId>
  <artifactId>pipeline-framework</artifactId>
  <version>LATEST_VERSION</version>
</dependency>
```

### Optional Dependencies for Persistence

If you plan to use the `autoPersist = true` feature for automatic entity persistence, you'll also need to include database dependencies:

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-reactive-pg-client</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-hibernate-reactive-panache</artifactId>
</dependency>
```

If you don't need persistence functionality, you can omit these dependencies and set `autoPersist = false` on all your pipeline steps.

## Basic Configuration

### 1. Define Your Protocol Buffer (Proto) Definitions

First, define your protocol buffer definitions in `src/main/proto/` that will be used for gRPC communication:

```protobuf
// src/main/proto/payment.proto
syntax = "proto3";

package com.example.payment;

option java_multiple_files = true;
option java_package = "com.example.grpc";
option java_outer_classname = "PaymentGrpc";

message PaymentRecordGrpc {
  string id = 1;
  string csv_id = 2;
  string recipient = 3;
  double amount = 4;
  string currency = 5;
}

message PaymentStatusGrpc {
  string id = 1;
  string payment_record_id = 2;
  string status = 3;
  string message = 4;
}

service ProcessPaymentService {
  rpc processPayment(PaymentRecordGrpc) returns (PaymentStatusGrpc) {}
}
```

### 2. Create Your Service Class

Create a class that implements one of the step interfaces:

```java
@PipelineStep(
   order = 1,
   inputType = PaymentRecord.class,
   outputType = PaymentStatus.class,
   stepType = StepOneToOne.class,
   backendType = GenericGrpcReactiveServiceAdapter.class,
   grpcStub = MutinyProcessPaymentServiceGrpc.MutinyProcessPaymentServiceStub.class,
   grpcImpl = MutinyProcessPaymentServiceGrpc.ProcessPaymentServiceImplBase.class,
   inboundMapper = PaymentRecordMapper.class,
   outboundMapper = PaymentStatusMapper.class,
   grpcClient = "process-payment",
   autoPersist = true,        // Enable auto-persistence (requires quarkus-reactive-pg-client dependency)
   debug = true
)
@ApplicationScoped
public class ProcessPaymentService implements StepOneToOne<PaymentRecord, PaymentStatus> {
    
    @Override
    public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
        // Your implementation here
        return Uni.createFrom().item(/* processed payment status */);
    }
}
```

### 3. Create Your Mapper Classes

Create mapper classes for converting between gRPC, DTO, and domain types using MapStruct. Mappers implement the `Mapper` interface with three generic types (gRPC, DTO, Domain):

```java
@Mapper(
    componentModel = "cdi",
    uses = {CommonConverters.class},
    unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface PaymentRecordMapper extends Mapper<PaymentRecordGrpc, PaymentRecordDto, PaymentRecord> {

    PaymentRecordMapper INSTANCE = Mappers.getMapper(PaymentRecordMapper.class);

    // Domain ↔ DTO
    @Override
    PaymentRecordDto toDto(PaymentRecord domain);

    @Override
    PaymentRecord fromDto(PaymentRecordDto dto);

    // DTO ↔ gRPC
    @Override
    @Mapping(target = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "currency", qualifiedByName = "currencyToString")
    PaymentRecordGrpc toGrpc(PaymentRecordDto dto);

    @Override
    @Mapping(target = "id", qualifiedByName = "stringToUUID")
    @Mapping(target = "amount", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "currency", qualifiedByName = "stringToCurrency")
    PaymentRecordDto fromGrpc(PaymentRecordGrpc grpc);
}
```

The MapStruct annotation processor automatically generates the implementation classes. You only need to define the interface methods with appropriate `@Mapping` annotations for complex transformations.

### 3. Define Your Protocol Buffer (Proto) Definitions

Before creating your services, define your protocol buffer (proto) definitions that will be used for gRPC communication between pipeline steps:

```protobuf
// src/main/proto/payment.proto
syntax = "proto3";

package com.example.payment;

option java_multiple_files = true;
option java_package = "com.example.grpc";
option java_outer_classname = "PaymentGrpc";

message PaymentRecordGrpc {
  string id = 1;
  string csv_id = 2;
  string recipient = 3;
  double amount = 4;
  string currency = 5;
}

message PaymentStatusGrpc {
  string id = 1;
  string payment_record_id = 2;
  string status = 3;
  string message = 4;
}

service ProcessPaymentService {
  rpc processPayment(PaymentRecordGrpc) returns (PaymentStatusGrpc) {}
}
```

### 4. Create Your Orchestrator Service

Create an orchestrator service that coordinates the pipeline:

```java
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

    @Override
    public void run() {
        processPipeline(csvFolder);
    }
    
    @Override
    public void processPipeline(String input) {
        // Process the input and create a stream
        Stream<CsvPaymentsInputFile> inputFileStream = processFolderService.process(input);
        Multi<CsvPaymentsInputFile> inputMulti = Multi.createFrom().iterable(inputFileStream::iterator);
        
        // Execute the pipeline with the generated steps
        executePipeline(inputMulti, List.of(/* steps are auto-discovered */));
    }
}
```

## Building Your Project

First, you'll need to add the gRPC Maven plugin to your `pom.xml` to generate Java classes from your proto definitions:

```xml
<build>
  <extensions>
    <extension>
      <groupId>kr.motd.maven</groupId>
      <artifactId>os-maven-plugin</artifactId>
      <version>1.7.1</version>
    </extension>
  </extensions>
  <plugins>
    <!-- Other plugins -->
  </plugins>
</build>
```

When you build your project, the framework will automatically generate the necessary adapters and register your step in the pipeline:

```bash
mvn clean compile
```

The build process will:
1. Generate gRPC classes from your proto definitions
2. Discover all `@PipelineStep` annotated services
3. Generate gRPC and REST adapters for each service
4. Create a complete pipeline application that orchestrates all steps
5. Register all generated components with the dependency injection container

## Running Your Application

Run your application using the standard Quarkus commands:

```bash
# Development mode
mvn quarkus:dev

# Production mode
mvn clean package
java -jar target/my-app-runner.jar

# Native mode
mvn clean package -Pnative
./target/my-app-runner
```

## Testing Your Application

The framework provides testing utilities to help you test your pipeline steps:

```java
@QuarkusTest
class ProcessPaymentServiceTest {

    @InjectMock ProcessFolderService processFolderService;
    
    @Test
    void testProcessPayment() {
        PaymentRecord testRecord = createTestPaymentRecord();
        
        Uni<PaymentStatus> result = service.applyOneToOne(testRecord);
        
        UniAssertSubscriber<PaymentStatus> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
            
        PaymentStatus status = subscriber.awaitItem().getItem();
        assertNotNull(status);
        assertEquals("PROCESSED", status.getStatus());
    }
}
```

## Next Steps

Once you have the basics working, explore these advanced topics:

- [Application Structure](/guide/application-structure): Learn how to structure complex pipeline applications
- [Backend Services](/guide/backend-services): Dive deeper into creating backend services
- [Orchestrator Services](/guide/orchestrator-services): Master orchestrator service creation
- [Error Handling & DLQ](/guide/error-handling): Implement robust error handling
- [Observability](/guide/observability): Monitor and observe your pipeline applications