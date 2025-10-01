# Annotations

The Pipeline Framework uses annotations to simplify configuration and automatic generation of pipeline components.

## @PipelineStep

The `@PipelineStep` annotation marks a class as a pipeline step and enables automatic generation of gRPC and REST adapters.

### Parameters

- `order`: Defines the order of execution for this step in the pipeline
- `inputType`: The input type for this step
- `outputType`: The output type for this step
- `stepType`: The step type (StepOneToOne, StepOneToMany, etc.)
- `backendType`: The backend adapter type (GenericGrpcReactiveServiceAdapter, etc.)
- `backpressureBufferCapacity`: The buffer capacity for backpressure handling (default: 1024)
- `backpressureStrategy`: The backpressure strategy ("BUFFER", "DROP"; default: "BUFFER")

Note: The "ERROR" strategy is not available in Mutiny 2.9.4. By default, Mutiny will signal an error when overflow occurs if no other overflow strategy is specified.
- `grpcStub`: The gRPC stub class for this pipeline step
- `grpcImpl`: The gRPC implementation class for this backend service
- `inboundMapper`: The inbound mapper class for this pipeline service/step
- `outboundMapper`: The outbound mapper class for this pipeline service/step
- `grpcClient`: The gRPC client name for this pipeline step
- `autoPersist`: Whether to enable auto-persistence for this step
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
   stepType = StepOneToOne.class,
   backendType = GenericGrpcReactiveServiceAdapter.class,
   grpcStub = MutinyProcessPaymentServiceGrpc.MutinyProcessPaymentServiceStub.class,
   grpcImpl = MutinyProcessPaymentServiceGrpc.ProcessPaymentServiceImplBase.class,
   inboundMapper = PaymentRecordInboundMapper.class,
   outboundMapper = PaymentStatusOutboundMapper.class,
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
   concurrency = 1000
)
@ApplicationScoped
public class ProcessPaymentService implements StepOneToOne<PaymentRecord, PaymentStatus> {
    // Implementation
}
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
3. Implement the mapper interfaces (`Mapper`)
4. Implement the service interface (`StepOneToOne`, etc.)

The framework automatically generates and registers the adapter beans at build time.