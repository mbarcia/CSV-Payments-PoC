# Annotation-Based Automatic Adapter Generation

This feature allows developers to automatically generate gRPC and REST adapters for pipeline steps by simply adding annotations to their service and mapper classes.

## Overview

The annotation-based adapter generation eliminates the need to manually create adapter classes, reducing boilerplate code and potential configuration errors. Developers only need to:

1. Annotate their service class with `@PipelineStep`
2. Annotate their mapper interface with `@MapperForStep`
3. Let the framework automatically generate and register the adapters

## Usage

### 1. Annotate Your Service Class

Add the `@PipelineStep` annotation to your service class:

```java
@ApplicationScoped
@PipelineStep(
    order = 1,
    autoPersist = true,
    debug = true,
    recoverOnFailure = true,
    inputType = PaymentRequest.class,
    outputType = PaymentResponse.class,
    stub = SendPaymentRecordServiceGrpc.SendPaymentRecordServiceImplBase.class
)
public class PaymentProcessingService extends ConfigurableStep 
        implements ReactiveService<PaymentRequest, PaymentResponse> {

    @Override
    public Uni<PaymentResponse> process(PaymentRequest request) {
        // Your business logic here
        return Uni.createFrom().item(() -> {
            PaymentResponse response = new PaymentResponse();
            response.setId(request.getId());
            response.setStatus("SUCCESS");
            response.setMessage("Payment processed successfully");
            return response;
        });
    }
}
```

### 2. Annotate Your Mapper Interface

Add the `@MapperForStep` annotation to your mapper interface:

```java
@Mapper(componentModel = "cdi")
@MapperForStep(
    order = 1,
    grpc = PaymentRecord.class,
    domain = PaymentRequest.class,
    domainInputType = PaymentRequest.class,
    domainOutputType = PaymentResponse.class
)
public interface PaymentProcessingMapper {
    
    PaymentRequest fromGrpc(PaymentRecord paymentRecord);
    
    AckPaymentSent toGrpc(PaymentResponse paymentResponse);
}
```

### 3. That's It!

The framework will automatically:
- Generate a gRPC adapter class at build time
- Register the adapter as a CDI bean
- Wire up the mapper and service dependencies
- Make the gRPC service available for use

## Benefits

1. **Reduced Boilerplate**: No need to manually create adapter classes
2. **Consistent Configuration**: Configuration is defined in one place
3. **Improved Developer Experience**: Simpler, more intuitive API
4. **Reduced Errors**: Less manual configuration reduces the chance of errors
5. **Better Maintainability**: Configuration changes only need to be made in one place