# Auto-Persistence Feature

The pipeline framework now includes an auto-persistence feature that automatically persists domain entities before they are processed by services. This feature is designed to be flexible and work with different database technologies.

## How It Works

The auto-persistence feature uses a provider-based architecture that supports both reactive (Hibernate Reactive) and blocking (JDBC) databases. The framework automatically detects which persistence provider to use based on the entity type.

## Enabling Auto-Persistence

Auto-persistence can be enabled at two levels:

1. **Global Level**: Set `csv-poc.pipeline.auto-persist=true` in your application.properties
2. **Step Level**: Configure individual steps with `stepConfig.autoPersist(true)`

## Supported Database Technologies

### Hibernate Reactive (Default)
The framework includes a built-in provider for Hibernate Reactive that works with Panache entities.

### JDBC
The framework includes a built-in provider for traditional JDBC-based persistence.

### Custom Providers
You can implement the `PersistenceProvider` interface to add support for other database technologies.

## Using Auto-Persistence in gRPC Services

To use auto-persistence in your gRPC services:

1. Extend one of the provided adapter classes:
   - `GrpcReactiveServiceAdapter`
   - `GrpcServiceStreamingAdapter`
   - `GrpcServiceClientStreamingAdapter`

2. Optionally override the `getStepConfig()` method to provide specific configuration:

```java
@Override
protected StepConfig getStepConfig() {
    return stepConfig; // Injected or created StepConfig
}
```

3. Optionally override the `isAutoPersistenceEnabled()` method for custom logic:

```java
@Override
protected boolean isAutoPersistenceEnabled() {
    return true; // or custom logic
}
```

## Using Auto-Persistence in REST Resources

To use auto-persistence in your REST resources:

1. Extend one of the provided adapter classes:
   - `RestReactiveServiceAdapter`
   - `RestReactiveStreamingServiceAdapter`

2. Use the provided methods:
   - `processWithAutoPersistence()` for single entities
   - `processStreamWithAutoPersistence()` for streams

## Creating Custom Entities

For your entities to work with auto-persistence:

1. **Hibernate Reactive**: Extend `PanacheEntityBase` or use JPA annotations
2. **JDBC**: Use standard JPA annotations

## Example Usage

Here's an example of how to modify a gRPC service to use auto-persistence:

```java
@GrpcService
public class MyGrpcService extends MutinyMyServiceGrpc.MyServiceImplBase {
    
    @Inject
    MyReactiveService domainService;
    
    @Override
    public Uni<MyGrpcResponse> remoteProcess(MyGrpcRequest request) {
        return new GrpcReactiveServiceAdapter<MyGrpcRequest, MyGrpcResponse, MyDomainEntity, MyDomainResult>() {
            @Override
            protected ReactiveService<MyDomainEntity, MyDomainResult> getService() {
                return domainService;
            }
            
            @Override
            protected MyDomainEntity fromGrpc(MyGrpcRequest grpcIn) {
                // Convert gRPC request to domain entity
                return myMapper.fromGrpc(grpcIn);
            }
            
            @Override
            protected MyGrpcResponse toGrpc(MyDomainResult domainOut) {
                // Convert domain result to gRPC response
                return myMapper.toGrpc(domainOut);
            }
            
            @Override
            protected StepConfig getStepConfig() {
                // Return configuration with auto-persistence enabled
                return new StepConfig().autoPersist(true);
            }
        }.remoteProcess(request);
    }
}
```

## Configuration Properties

The following properties can be set in application.properties:

- `csv-poc.pipeline.auto-persist`: Enable/disable auto-persistence globally (default: true)
- `csv-poc.pipeline.concurrency`: Concurrency level for processing
- `csv-poc.pipeline.retry-limit`: Number of retry attempts
- `csv-poc.pipeline.retry-wait-ms`: Initial wait time between retries
- `csv-poc.pipeline.debug`: Enable debug logging
- `csv-poc.pipeline.recover-on-failure`: Continue processing on failure
- `csv-poc.pipeline.run-with-virtual-threads`: Use virtual threads
- `csv-poc.pipeline.max-backoff-ms`: Maximum backoff time
- `csv-poc.pipeline.jitter`: Add jitter to retry delays