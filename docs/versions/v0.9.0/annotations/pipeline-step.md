# @PipelineStep Annotation

The `@PipelineStep` annotation is used to mark a class as a pipeline step. This annotation enables automatic generation of gRPC and REST adapters.

## Parameters

- `order`: Defines the order of execution for this step in the pipeline
- `autoPersist`: Whether to automatically persist the results of this step. Note: When set to `true`, requires the `quarkus-reactive-pg-client` dependency and related Hibernate Reactive dependencies for database persistence functionality.
- `debug`: Enables debug mode for this step
- `recoverOnFailure`: Whether to attempt recovery if this step fails
- `stub`: The gRPC stub class to use for this step
- `inboundMapper`: The MapStruct-based mapper class to convert incoming requests (using unified Mapper interface)
- `outboundMapper`: The MapStruct-based mapper class to convert outgoing responses (using unified Mapper interface)
- `grpcType`: The gRPC type this mapper handles
- `domainType`: The domain type this mapper handles
- `local`: Whether this step runs locally in the same process (default: false). When `true`, the step runs in the same application process without requiring gRPC communication, making it suitable for services that process data locally within the orchestrator.

## Example Usage

```java
@PipelineStep(
   order = 1,
   autoPersist = true,
   debug = true,
   recoverOnFailure = true,
   stub = MyGrpc.MyStub.class,
   inboundMapper = MyMapper.class,
   outboundMapper = MyMapper.class,
   grpcType = PaymentsProcessingSvc.AckPaymentSent.class,
   domainType = AckPaymentSent.class

)
public class MyPipelineStep implements StepOneToOne<Input, Output> {
    // Implementation here
}
```

This annotation eliminates the need for manual adapter configuration, reducing boilerplate code and improving maintainability.

### Benefits

1. **Type Safety**: Ensures correct mapping between types at compile time
2. **Automatic Registration**: Mappers are automatically discovered and registered
3. **Consistent Interface**: All mappers implement the same base interfaces using MapStruct
4. **Reduced Configuration**: Eliminates the need for manual mapper configuration
5. **Code Generation**: MapStruct automatically generates efficient mapping implementations