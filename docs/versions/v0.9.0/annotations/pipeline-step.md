# @PipelineStep Annotation

The `@PipelineStep` annotation is used to mark a class as a pipeline step. This annotation enables automatic generation of gRPC and REST adapters.

## Parameters

- `order`: Defines the order of execution for this step in the pipeline
- `inputType`: The input type for this step (domain type)
- `outputType`: The output type for this step (domain type)
- `inputGrpcType`: The gRPC input type for this pipeline step. This is used for gRPC client generation and specifies the exact gRPC message type to be used in the generated client step interface instead of inferring from the domain type.
- `outputGrpcType`: The gRPC output type for this pipeline step. This is used for gRPC client generation and specifies the exact gRPC message type to be used in the generated client step interface instead of inferring from the domain type.
- `stepType`: The step type (StepOneToOne, StepOneToMany, etc.)
- `backendType`: The backend adapter type (GenericGrpcReactiveServiceAdapter, etc.)
- `backpressureBufferCapacity`: The buffer capacity for backpressure handling (default: 1024)
- `backpressureStrategy`: The backpressure strategy ("BUFFER", "DROP"; default: "BUFFER")
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
- `parallel`:  Whether to enable parallel processing for this step

## Example Usage

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
   parallel = true
)
@ApplicationScoped
public class ProcessPaymentService implements StepOneToOne<PaymentRecord, PaymentStatus> {
    // Implementation here
}
```

This annotation eliminates the need for manual adapter configuration, reducing boilerplate code and improving maintainability.

### Usage Notes

The `inputGrpcType` and `outputGrpcType` parameters allow you to explicitly specify the gRPC message types that should be used in the generated client step interfaces. When these parameters are provided, the framework will use these exact types in the generated step implementations instead of trying to infer them from the domain types. This gives you more control over the interface contracts and helps avoid issues where the inferred types don't match the expected gRPC service contract.

### Benefits

1. **Type Safety**: Ensures correct mapping between types at compile time
2. **Automatic Registration**: Mappers are automatically discovered and registered
3. **Consistent Interface**: All mappers implement the same base interfaces using MapStruct
4. **Reduced Configuration**: Eliminates the need for manual mapper configuration
5. **Code Generation**: MapStruct automatically generates efficient mapping implementations