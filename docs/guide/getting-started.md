# Getting Started

This guide will help you set up and start using the Pipeline Framework in your project.

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- A Quarkus-based project (the framework is designed to work with Quarkus)

## Adding the Framework to Your Project

Add the following dependencies to your `pom.xml`:

```xml
<dependency>
  <groupId>io.github.mbarcia</groupId>
  <artifactId>pipeline-framework-runtime</artifactId>
  <version>LATEST_VERSION</version>
</dependency>

<dependency>
  <groupId>io.github.mbarcia</groupId>
  <artifactId>pipeline-framework-deployment</artifactId>
  <version>LATEST_VERSION</version>
  <scope>provided</scope>
</dependency>
```

## Basic Configuration

### 1. Create Your Service Class

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
   inboundMapper = PaymentRecordInboundMapper.class,
   outboundMapper = PaymentStatusOutboundMapper.class,
   grpcClient = "process-payment",
   autoPersist = true,
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

### 2. Create Your Mapper Classes

Create mapper classes for converting between different representations:

```java
@MapperForStep(
    order = 1,
    grpcType = PaymentRecordGrpc.class,
    domainType = PaymentRecord.class
)
@ApplicationScoped
public class PaymentRecordInboundMapper implements InboundMapper<PaymentRecordGrpc, PaymentRecord> {
    
    @Override
    public PaymentRecord fromGrpc(PaymentRecordGrpc grpc) {
        // Mapping implementation
        return PaymentRecord.builder()
            .id(UUID.fromString(grpc.getId()))
            .csvId(grpc.getCsvId())
            .recipient(grpc.getRecipient())
            .amount(new BigDecimal(grpc.getAmount()))
            .currency(Currency.getInstance(grpc.getCurrency()))
            .build();
    }
}

@MapperForStep(
    order = 2,
    grpcType = PaymentStatusGrpc.class,
    domainType = PaymentStatus.class
)
@ApplicationScoped
public class PaymentStatusOutboundMapper implements OutboundMapper<PaymentStatus, PaymentStatusGrpc> {
    
    @Override
    public PaymentStatusGrpc toGrpc(PaymentStatus domain) {
        // Mapping implementation
        return PaymentStatusGrpc.newBuilder()
            .setId(domain.getId().toString())
            .setPaymentRecordId(domain.getPaymentRecord().getId().toString())
            .setStatus(domain.getStatus())
            .setMessage(domain.getMessage())
            .build();
    }
}
```

### 3. Create Your Orchestrator Service

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

When you build your project, the framework will automatically generate the necessary adapters and register your step in the pipeline:

```bash
mvn clean compile
```

The annotation processor will:
1. Discover all `@PipelineStep` annotated services
2. Generate gRPC and REST adapters for each service
3. Create a complete pipeline application that orchestrates all steps
4. Register all generated components with the dependency injection container

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