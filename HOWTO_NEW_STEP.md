# How to Create a New Pipeline Step

This document explains how to create a new step for the pipeline framework in the `io.github.mbarcia.pipeline*` packages. The pipeline framework is designed to provide a robust, scalable, and maintainable way to process data through a series of steps with built-in benefits for high-throughput, distributed systems.

## Project Structure

Your project will need to have:
- a common package (as a maven submodule)
- step packages (as more maven submodules)
- a parent POM and docker-compose.yaml
- optionally, a TLS cert

The common module will hold all your entities/dtos and mappers. Also, your .proto definitions for your services.
Each rpc service detailed there will probably be a step of the pipeline.

Then for each step, you will have to write a maven submodule with:
- an implementation of "ReactiveService" (this is your actual business logic). You will need to deal with Mutiny and return `Uni<S>` from your `process()` method.
- a gRPC wrapper of that service using `GrpcReactiveServiceAdapter`
- an `application.properties` file
- an optional REST resource class if you want to expose it also to a REST api.

To properly expose your service through gRPC, you should create a gRPC service class that extends the generated gRPC service base class and uses `GrpcReactiveServiceAdapter` either as an anonymous class (like in `PersistCsvPaymentsOutputFileGrpcService`) or as a separate class. This approach ensures proper error handling and integration with the pipeline framework.

For REST exposure, create a JAX-RS resource class that injects your service and exposes endpoints with proper error handling and response formatting.

Requirements for the POM dependencies, both for the common submodule and the "step" submodule.

## Creating a New Step

### Step 1: Define Your Business Logic
Create a service class that implements your business logic. This class should focus purely on the business requirements without worrying about pipeline concerns.

```java
@ApplicationScoped
public class SimpleBusinessService implements ReactiveService<InputType, OutputType> {
    
    @Override
    public Uni<OutputType> process(InputType input) {
        // Simple business logic
        OutputType result = doSimpleProcessing(input);
        
        // Wrap the result in Uni
        return Uni.createFrom().item(result);
    }
    
    private OutputType doSimpleProcessing(InputType input) {
        // Your actual business logic here
        return new OutputType(input.getValue());
    }
}
```
Note: you must wrap your business object with a Uni, and return `Uni<S>`

### Step 2: Rinse and repeat
At this point, you can just keep adding all your backend services into Maven modules, and the entities/dto/mappers that go with each of them, to the common module.
It is at this point where you can also provide unit tests (and integration tests) for each one.
Alternatively, you can choose to postpone the remaining backend services and start with the client-side implementation right away.

### Step 2: Implement a client-side orchestration of PipelineSteps
In order to build the client of your backend services, there is a host of options. You can use a React UI to call the REST endpoints, or you can build a CLI process that might make use of another backend service (the "orchestrator").

A typical implementation of this backend orchestrator would be:
```java
  public Uni<Void> process(String csvFolderPath) throws URISyntaxException {
    // Create a single pipeline for processing payment records
    List<PipelineStep<?, ?>> paymentProcessingSteps = List.of(
        persistAndSendPaymentStep,
        processAckPaymentStep,
        processPaymentStatusStep
    );
    
    GenericPipelineService<InputCsvFileProcessingSvc.PaymentRecord, PaymentStatusSvc.PaymentOutput> paymentPipeline =
        new GenericPipelineService<>(
            VIRTUAL_EXECUTOR,
            config,
            paymentProcessingSteps
        );
    
    // First, process the folder to get a stream of input files
    Uni<Multi<CsvPaymentsInputFile>> inputFilesUni = processFolderStep.execute(csvFolderPath);
    
    // Then, for each input file, process it through the payment pipeline
    return inputFilesUni
        .onItem()
        .transformToMulti(files -> files)
        .onItem()
        .transformToUniAndMerge(inputFile -> {
          
          // First, process the input file to get a stream of PaymentRecord objects
          Uni<Multi<InputCsvFileProcessingSvc.PaymentRecord>> paymentRecordsUni =
              processInputFileStep.execute(inputFile);

          // Then, for each PaymentRecord, process it through the payment pipeline
          Multi<PaymentStatusSvc.PaymentOutput> paymentOutputsMulti =
              paymentRecordsUni
                  .onItem()
                  .transformToMulti(records -> records)
                  .onItem()
                  .transformToUniAndMerge(paymentPipeline::process);

          // Finally, process the output file with all the payment outputs
          return processOutputFileStep.execute(paymentOutputsMulti);
        })
        .collect()
        .asList()
        .onItem()
        .transformToUni(list -> Uni.createFrom().voidItem());
  }
}
```

Choose the appropriate step interface based on your input/output cardinality:

```java
@ApplicationScoped
public class MyProcessingStep implements UniToUniStep<InputType, OutputType> {
    
    @Inject
    MyBusinessService myBusinessService;
    
    @Override
    public Uni<OutputType> execute(InputType input) {
        // Your business logic here
        return myBusinessService.process(input)
            .onItem().transform(result -> new OutputType(result));
    }
}
```

### Step 3: Configure Execution Properties (Optional)
Override the `getExecutionConfig()` method to customize execution behavior:

```java
@Override
public StepExecutionConfig getExecutionConfig() {
    return new StepExecutionConfig(false); // Disable retries for this step
}
```

## Pipeline Framework Overview

The pipeline framework is built around several key concepts:

### 1. PipelineStep Interface
The base interface that all steps must implement. It defines a single method `execute(IN input)` that returns a `Uni<OUT>` (a reactive type representing a single value).

### 2. Specialized Step Types
The framework provides specialized interfaces for different cardinality transformations:

- **UniToUniStep<IN, OUT>**: Transforms one input into one output (1:1)
- **UniToMultiStep<IN, OUT>**: Transforms one input into multiple outputs (1:N)
- **MultiToUniStep<IN, OUT>**: Aggregates multiple inputs into one output (N:1)

### 3. GenericPipelineService
The engine that executes a series of steps. It handles:
- Thread management using virtual threads
- Error handling and logging
- Retry logic with exponential backoff
- Step chaining

## Step Chaining and Immutability

An important concept in the pipeline framework is that each step's output type is the next step's input type. Steps are chained together, and each object is immutable. This means there is never a single SQL UPDATE command issued to the database.

Additionally, persistence to the database is another _optional_ service in the pipeline. Data needs to be carried over from one step to the next if it's going to be needed at any point in a subsequent step. Nevertheless, this carrying over is optional, as any step could potentially make a query to the database for any previous information that may have been "lost in translation".

## Example Implementation

Here's a complete example of a simple processing step:

```java
@ApplicationScoped
public class PaymentValidationStep implements UniToUniStep<PaymentRequest, ValidatedPayment> {
    
    private static final Logger LOG = LoggerFactory.getLogger(PaymentValidationStep.class);
    
    @Inject
    PaymentValidationService validationService;
    
    @Override
    public Uni<ValidatedPayment> execute(PaymentRequest request) {
        LOG.debug("Validating payment: {}", request.getId());
        
        return validationService.validate(request)
            .onItem().transform(validated -> {
                LOG.debug("Payment validated: {}", request.getId());
                return new ValidatedPayment(request, validated.isValid());
            })
            .onFailure().invoke(e -> 
                LOG.error("Failed to validate payment: {}", request.getId(), e));
    }
}
```

## Exposing Services Through gRPC and REST

Once you've implemented your pipeline step, you may want to expose it through gRPC and/or REST interfaces. The pipeline framework provides patterns for both:

### gRPC Exposure

To expose your service through gRPC, create a service class that extends the generated gRPC service base class and uses `GrpcReactiveServiceAdapter`. You can either use an anonymous class (as seen in `PersistCsvPaymentsOutputFileGrpcService`) or create a separate adapter class.

Example using anonymous class approach:
```java
@GrpcService
public class YourGrpcService extends MutinyYourServiceGrpc.YourServiceImplBase {

    @Inject YourReactiveService domainService;
    @Inject YourMapper mapper;

    private final GrpcReactiveServiceAdapter<YourGrpcRequest, YourGrpcResponse, YourDomainIn, YourDomainOut> adapter =
            new GrpcReactiveServiceAdapter<>() {
                @Override
                protected YourReactiveService getService() {
                    return domainService;
                }

                @Override
                protected YourDomainIn fromGrpc(YourGrpcRequest grpcIn) {
                    return mapper.fromGrpc(grpcIn);
                }

                @Override
                protected YourGrpcResponse toGrpc(YourDomainOut domainOut) {
                    return mapper.toGrpc(domainOut);
                }
            };

    @Override
    public Uni<YourGrpcResponse> process(YourGrpcRequest request) {
        return adapter.remoteProcess(request);
    }
}
```

### REST Exposure

To expose your service through REST, create a JAX-RS resource class:

```java
@Path("/your-service")
public class YourResource {

  @Inject YourReactiveService yourService;

  @POST
  @Path("/process")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Uni<Response> process(YourRequestDto request) {
    try {
      return yourService.process(request.toDomain())
          .onItem().transform(domainResult -> Response.ok().entity(YourResponseDto.fromDomain(domainResult)).build())
          .onFailure().recoverWithItem(e -> {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Processing failed: " + e.getMessage())).build();
          });
    } catch (Exception e) {
      return Uni.createFrom().item(
          Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse("Processing failed: " + e.getMessage())).build());
    }
  }
}
```

### Client Usage

For services that need to call other services, configure gRPC clients in your `application.properties`:

```properties
quarkus.grpc.clients.your-service.host=localhost
quarkus.grpc.clients.your-service.port=8443
quarkus.grpc.clients.your-service.plain-text=false
quarkus.grpc.clients.your-service.use-quarkus-grpc-client=true
quarkus.grpc.clients.your-service.tls.enabled=true
```

Then inject the client in your service:

```java
@Inject
@GrpcClient("your-service")
MutinyYourServiceGrpc.MutinyYourServiceStub yourServiceClient;
```

### Configuration

Steps can be configured using the pipeline configuration system. See [PIPELINE_CONFIGURATION.md](./orchestrator-svc/PIPELINE_CONFIGURATION.md) for detailed information on how to configure steps with profiles, defaults, and overrides.

This step automatically benefits from:
- Virtual thread execution
- Retry logic (3 attempts by default)
- Structured logging
- Error handling
- Metrics collection
- Distributed tracing

All without any additional code in your business logic.

