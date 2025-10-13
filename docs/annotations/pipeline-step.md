# Annotations

The Pipeline Framework uses annotations to simplify configuration and automatic generation of pipeline components.

## @PipelineStep

The `@PipelineStep` annotation marks a class as a pipeline step and enables automatic generation of gRPC and REST adapters.

### Parameters

- `order`: Defines the order of execution for this step in the pipeline
- `inputType`: The input type for this step (domain type)
- `outputType`: The output type for this step (domain type)
- `inputGrpcType`: The gRPC input type for this pipeline step. This is used for gRPC client generation and specifies the exact gRPC message type to be used in the generated client step interface instead of inferring from the domain type.
- `outputGrpcType`: The gRPC output type for this pipeline step. This is used for gRPC client generation and specifies the exact gRPC message type to be used in the generated client step interface instead of inferring from the domain type.
- `stepType`: The step type (StepOneToOne, StepOneToMany, etc.)
- `backendType`: The backend adapter type (GenericGrpcReactiveServiceAdapter, etc.)
- `backpressureBufferCapacity`: The buffer capacity for backpressure handling (default: 1024)
- `backpressureStrategy`: The backpressure strategy ("BUFFER", "DROP"; default: "BUFFER")
- `batchSize`: The batch size for collecting inputs before processing (default: 10). For StepManyToOne steps, this controls how many input items are collected before the batch is processed. Set to a value larger than the expected number of related items to ensure they are processed together.
- `batchTimeoutMs`: The time window in milliseconds to wait before processing a batch, even if the batch size hasn't been reached (default: 1000ms). For StepManyToOne steps, this controls how long to wait for additional items to accumulate in a batch.

Note: The "ERROR" strategy is not available in Mutiny 2.9.4. By default, Mutiny will signal an error when overflow occurs if no other overflow strategy is specified.
- `grpcStub`: The gRPC stub class for this pipeline step
- `grpcImpl`: The gRPC implementation class for this backend service
- `inboundMapper`: The inbound mapper class for this pipeline service/step - handles conversion from gRPC to domain types (using MapStruct-based unified Mapper interface)
- `outboundMapper`: The outbound mapper class for this pipeline service/step - handles conversion from domain to gRPC types (using MapStruct-based unified Mapper interface)
- `grpcClient`: The gRPC client name for this pipeline step
- `autoPersist`: Whether to enable auto-persistence for this step. Note: When set to `true`, requires the `quarkus-reactive-pg-client` dependency and related Hibernate Reactive dependencies for database persistence functionality.
- `debug`: Whether to enable debug mode for this step
- `recoverOnFailure`: Whether to enable failure recovery for this step
- `grpcEnabled`: Whether to enable gRPC adapter generation for this step
- `local`: Whether this step runs locally in the same process (default: false). When `true`, the step runs in the same application process without requiring gRPC communication, making it suitable for services that process data locally within the orchestrator.
- `restEnabled`: Whether to enable REST adapter generation for this step
- `runWithVirtualThreads`: Whether to run with virtual threads
- `retryLimit`: The retry limit for this step
- `retryWait`: The retry wait time for this step
- `maxBackoff`: The maximum backoff time for this step
- `jitter`: Whether to enable jitter for this step
- `concurrency`: The concurrency limit for this step

### Example

```java
@PipelineStep(
   order = 1,
   inputType = PaymentRecord.class,
   outputType = PaymentStatus.class,
   inputGrpcType = PaymentsProcessingSvc.PaymentRecord.class,
   outputGrpcType = PaymentsProcessingSvc.PaymentStatus.class,
   stepType = StepOneToOne.class,
   backendType = GenericGrpcReactiveServiceAdapter.class,
   grpcStub = MutinyProcessPaymentServiceGrpc.MutinyProcessPaymentServiceStub.class,
   grpcImpl = MutinyProcessPaymentServiceGrpc.ProcessPaymentServiceImplBase.class,
   inboundMapper = PaymentRecordMapper.class,
   outboundMapper = PaymentStatusMapper.class,
   grpcClient = "process-payment",
   autoPersist = true,
   debug = true,
   recoverOnFailure = true,
   grpcEnabled = true,
   restEnabled = false,
   runWithVirtualThreads = true,
   retryLimit = 3,
   retryWait = "PT500MS",
   maxBackoff = "PT30S",
   jitter = true,
   concurrency = 1000,
   batchSize = 50,
   batchTimeoutMs = 5000
)
@ApplicationScoped
public class ProcessPaymentService implements StepOneToOne<PaymentRecord, PaymentStatus> {
    // Implementation
}
```

### Usage Notes

The `inputGrpcType` and `outputGrpcType` parameters allow you to explicitly specify the gRPC message types that should be used in the generated client step interfaces. When these parameters are provided, the framework will use these exact types in the generated step implementations instead of trying to infer them from the domain types. This gives you more control over the interface contracts and helps avoid issues where the inferred types don't match the expected gRPC service contract.

### Batching Configuration for StepManyToOne

For `StepManyToOne` steps, the `batchSize` and `batchTimeoutMs` parameters control how input items are collected before processing:

- **batchSize**: Controls how many input items are collected before the batch is processed. For scenarios where related records should be processed together (such as all PaymentOutput records from the same CSV file), set this to a value larger than the expected number of related items to ensure they are processed in the same batch.

- **batchTimeoutMs**: Controls how long to wait for additional items to accumulate in a batch before processing, even if the batch size hasn't been reached. This is particularly useful when dealing with streams where items arrive sporadically.

For example, to ensure that all 12 PaymentOutput records from a single CSV file are processed together in one batch, configure a batch size of 50 and sufficient timeout:
```java
@PipelineStep(
   // ... other parameters
   stepType = StepManyToOne.class,
   batchSize = 50,        // Large enough to hold all related records
   batchTimeoutMs = 5000  // 5 second timeout to allow accumulation
)
```

## Benefits

1. **Reduced Boilerplate**: Developers no longer need to manually configure adapters
2. **Consistent Configuration**: Configuration is defined in one place (the step implementation)
3. **Improved Developer Experience**: Simpler, more intuitive API
4. **Reduced Errors**: Less manual configuration reduces the chance of errors
5. **Better Maintainability**: Configuration changes only need to be made in one place
6. **Architectural Integrity**: Maintains proper separation of concerns between orchestrator and service modules

## Usage

Developers only need to:

1. Annotate their service class with `@PipelineStep`
2. Create MapStruct-based mapper interfaces that extend the `Mapper<Grpc, Dto, Domain>` interface
3. Implement the service interface (`StepOneToOne`, etc.)

The framework automatically generates and registers the adapter beans at build time.