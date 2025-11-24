# Annotations

The Pipeline Framework uses annotations to simplify configuration and automatic generation of pipeline components.

## @PipelineStep

The `@PipelineStep` annotation marks a class as a pipeline step and enables automatic generation of gRPC and REST adapters. The framework follows an immutable architecture where no database updates occur during pipeline execution - only appends/preserves.

### Parameters

- `inputType`: The input type for this step (domain type)
- `outputType`: The output type for this step (domain type)
- `inputGrpcType`: The gRPC input type for this pipeline step. This is used for gRPC client generation and specifies the exact gRPC message type to be used in the generated client step interface instead of inferring from the domain type.
- `outputGrpcType`: The gRPC output type for this pipeline step. This is used for gRPC client generation and specifies the exact gRPC message type to be used in the generated client step interface instead of inferring from the domain type.
- `stepType`: The step type (StepOneToOne, StepOneToMany, StepManyToOne, StepManyToMany, StepSideEffect, or blocking variants)
- `backendType`: The backend adapter type (GenericGrpcReactiveServiceAdapter, etc.)
- `grpcStub`: The gRPC stub class for this pipeline step
- `grpcImpl`: The gRPC implementation class for this backend service
- `inboundMapper`: The inbound mapper class for this pipeline service/step - handles conversion from gRPC to domain types (using MapStruct-based unified Mapper interface)
- `outboundMapper`: The outbound mapper class for this pipeline service/step - handles conversion from domain to gRPC types (using MapStruct-based unified Mapper interface)
- `autoPersist`: When true, the input gets written to the DB during transformation (immutable architecture)
- `runOnVirtualThreads`: Whether to offload server processing to virtual threads, i.e. for I/O-bound operations
- `recoverOnFailure`: When true, enables dead letter queue (DLQ) support for error handling
- `backpressureBufferCapacity`: Buffer capacity when using BUFFER strategy
- `backpressureStrategy`: Backpressure strategy (BUFFER, DROP, or ERROR)
- `grpcClient`: The gRPC client name for this pipeline step
- `grpcEnabled`: Whether to enable gRPC adapter generation for this step
- `local`: Whether this step runs locally in the same process (default: false). When `true`, the step runs in the same application process without requiring gRPC communication, making it suitable for services that process data locally within the orchestrator.
- `restEnabled`: Whether to enable REST adapter generation for this step

### Example

```java
@PipelineStep(
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
   grpcEnabled = true,
   restEnabled = false,
   runOnVirtualThreads = true,
)
@ApplicationScoped
public class ProcessPaymentService implements StepOneToOne<PaymentRecord, PaymentStatus> {
    // Implementation
}
```

### Usage Notes

The `inputGrpcType` and `outputGrpcType` parameters allow you to explicitly specify the gRPC message types that should be used in the generated client step interfaces. When these parameters are provided, the framework will use these exact types in the generated step implementations instead of trying to infer them from the domain types. This gives you more control over the interface contracts and helps avoid issues where the inferred types don't match the expected gRPC service contract.

## Understanding parallel vs runOnVirtualThreads

It's important to understand the difference between these two configuration options:

- **`parallel`**: For client-side steps, enables concurrent processing of multiple items from the same stream. This translates into a flatMap() call (when parallel=true) or a concatMap() call (when parallel=false)
- **`runOnVirtualThreads`**: For server-side gRPC services, enables execution of `process()` code on virtual threads for better I/O handling

## Usage

Developers only need to:

1. Annotate their service class with `@PipelineStep`
2. Create MapStruct-based mapper interfaces that extend the `Mapper<Grpc, Dto, Domain>` interface
3. Implement the service interface (`StepOneToOne`, etc.)

The framework automatically generates and registers the adapter beans at build time.
