# Creating Backend Services

This guide explains how to create backend services using the Pipeline Framework, following the patterns demonstrated in the CSV Payments reference implementation.

## Overview

Backend services implement individual pipeline steps that process data as it flows through the pipeline. Each service focuses on a specific transformation or operation, promoting loose coupling and high cohesion.

## Service Creation Steps

### 1. Define the Service Interface

First, determine what type of step your service implements:
- `StepOneToOne<I, O>`: Transforms one input to one output
- `StepOneToMany<I, O>`: Transforms one input to multiple outputs
- `StepManyToOne<I, O>`: Aggregates multiple inputs to one output
- `StepManyToMany<I, O>`: Transforms multiple inputs to multiple outputs
- `StepSideEffect<I, O>`: Performs side effects without changing the data flow

### 2. Create the Service Class

Create your service class with the `@PipelineStep` annotation:

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
        // Implementation here
        return Uni.createFrom().item(/* processed payment status */);
    }
}
```

### 3. Implement the Business Logic

The core of your service is the implementation of the step interface method:

```java
@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    // Validate input
    if (paymentRecord == null) {
        return Uni.createFrom().failure(new IllegalArgumentException("Payment record cannot be null"));
    }
    
    // Perform business logic
    return processPayment(paymentRecord)
        .onItem().transform(result -> createPaymentStatus(paymentRecord, result))
        .onFailure().recoverWithUni(error -> {
            // Handle errors appropriately
            LOG.error("Failed to process payment: {}", error.getMessage(), error);
            return Uni.createFrom().item(createErrorStatus(paymentRecord, error));
        });
}

private Uni<PaymentProcessingResult> processPayment(PaymentRecord record) {
    // Implementation details
    // This might involve calling external services, database operations, etc.
    return Uni.createFrom().item(/* processing result */);
}

private PaymentStatus createPaymentStatus(PaymentRecord record, PaymentProcessingResult result) {
    // Create success status
    return PaymentStatus.builder()
        .paymentRecord(record)
        .status("SUCCESS")
        .message("Payment processed successfully")
        .build();
}

private PaymentStatus createErrorStatus(PaymentRecord record, Throwable error) {
    // Create error status
    return PaymentStatus.builder()
        .paymentRecord(record)
        .status("ERROR")
        .message("Payment processing failed: " + error.getMessage())
        .build();
}
```

## Mapper Creation

### Inbound Mapper
Converts gRPC objects to domain objects:

```java
@ApplicationScoped
public class PaymentRecordInboundMapper implements InboundMapper<PaymentRecordGrpc, PaymentRecord> {
    
    @Override
    public PaymentRecord fromGrpc(PaymentRecordGrpc grpc) {
        return PaymentRecord.builder()
            .id(UUID.fromString(grpc.getId()))
            .csvId(grpc.getCsvId())
            .recipient(grpc.getRecipient())
            .amount(new BigDecimal(grpc.getAmount()))
            .currency(Currency.getInstance(grpc.getCurrency()))
            .build();
    }
}
```

### Outbound Mapper
Converts domain objects to gRPC objects:

```java
@ApplicationScoped
public class PaymentStatusOutboundMapper implements OutboundMapper<PaymentStatus, PaymentStatusGrpc> {
    
    @Override
    public PaymentStatusGrpc toGrpc(PaymentStatus domain) {
        return PaymentStatusGrpc.newBuilder()
            .setId(domain.getId().toString())
            .setPaymentRecordId(domain.getPaymentRecord().getId().toString())
            .setStatus(domain.getStatus())
            .setMessage(domain.getMessage())
            .build();
    }
}
```

## Working with DTOs

When your service needs to work with DTOs internally, use the main mapper to convert between domain and DTO objects:

```java
@PipelineStep(
    // ... annotation configuration
)
@ApplicationScoped
public class ProcessPaymentService implements StepOneToOne<PaymentRecord, PaymentStatus> {
    
    @Inject
    PaymentRecordMapper paymentRecordMapper;  // Main mapper for DTO conversions
    
    @Override
    public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
        // Convert domain to DTO for internal processing
        PaymentRecordDto dto = paymentRecordMapper.toDto(paymentRecord);
        
        // Process the DTO
        PaymentStatusDto statusDto = processPaymentDto(dto);
        
        // Convert DTO back to domain
        return Uni.createFrom().item(paymentRecordMapper.fromDto(statusDto));
    }
    
    private PaymentStatusDto processPaymentDto(PaymentRecordDto dto) {
        // Process the DTO and return a status DTO
        return PaymentStatusDto.builder()
            .paymentRecordId(dto.getId())
            .status("PROCESSED")
            .message("Payment record processed successfully")
            .build();
    }
}
```

## Configuration and Customization

### Step Configuration
Customize step behavior through configuration:

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
    retryLimit = 5,
    retryWait = "PT1S",
    maxBackoff = "PT30S",
    jitter = true
)
```

### Runtime Configuration
Modify configuration at runtime:

```java
@Inject
ConfigurableStep step;  // Injected step instance

public void updateConfiguration() {
    step.liveConfig()
        .retryLimit(10)
        .retryWait(Duration.ofSeconds(2))
        .debug(true);
}
```

## Error Handling and Recovery

### Dead Letter Queue
Implement DLQ for failed items:

```java
@Override
public Uni<PaymentStatus> deadLetter(PaymentRecord paymentRecord, Throwable error) {
    LOG.warn("Sending failed payment record to dead letter queue: {}", paymentRecord.getId(), error);
    
    // Send to DLQ (e.g., database, message queue, file)
    return persistToDeadLetterQueue(paymentRecord, error)
        .onItem().transform(v -> createDlqStatus(paymentRecord));
}

private Uni<Void> persistToDeadLetterQueue(PaymentRecord record, Throwable error) {
    // Implementation to persist failed items
    return Uni.createFrom().voidItem();
}

private PaymentStatus createDlqStatus(PaymentRecord record) {
    return PaymentStatus.builder()
        .paymentRecord(record)
        .status("DLQ")
        .message("Moved to dead letter queue after failed processing")
        .build();
}
```

### Circuit Breaker Pattern
Implement circuit breaker for external service calls:

```java
private Uni<ExternalServiceResponse> callExternalService(PaymentRecord record) {
    return Uni.createFrom().item(/* external service call */)
        .onFailure().retry().withBackOff(Duration.ofMillis(100), Duration.ofSeconds(5))
        .atMost(3)
        .onFailure().recoverWithUni(error -> {
            LOG.error("External service call failed after retries", error);
            return Uni.createFrom().failure(new ServiceUnavailableException("External service unavailable", error));
        });
}
```

## Testing

### Unit Testing
Test your service logic in isolation:

```java
@QuarkusTest
class ProcessPaymentServiceTest {
    
    @Inject
    ProcessPaymentService service;
    
    @Test
    void testSuccessfulPaymentProcessing() {
        PaymentRecord record = createTestPaymentRecord();
        
        Uni<PaymentStatus> result = service.applyOneToOne(record);
        
        UniAssertSubscriber<PaymentStatus> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
            
        PaymentStatus status = subscriber.awaitItem().getItem();
        
        assertEquals("SUCCESS", status.getStatus());
        assertEquals(record.getId(), status.getPaymentRecord().getId());
    }
    
    @Test
    void testErrorHandling() {
        PaymentRecord invalidRecord = null;
        
        Uni<PaymentStatus> result = service.applyOneToOne(invalidRecord);
        
        UniAssertSubscriber<PaymentStatus> subscriber = 
            result.subscribe().withSubscriber(UniAssertSubscriber.create());
            
        subscriber.assertFailedWith(IllegalArgumentException.class);
    }
    
    private PaymentRecord createTestPaymentRecord() {
        return PaymentRecord.builder()
            .id(UUID.randomUUID())
            .csvId("test-csv-123")
            .recipient("John Doe")
            .amount(new BigDecimal("100.00"))
            .currency(Currency.getInstance("USD"))
            .build();
    }
}
```

### Integration Testing
Test the complete service with the framework:

```java
@QuarkusTest
class ProcessPaymentServiceIntegrationTest {
    
    @Inject
    ProcessPaymentService service;
    
    @Test
    void testFullPipelineIntegration() {
        // Test the service as part of the pipeline
        // This would involve testing with the generated pipeline application
    }
}
```

## Monitoring and Observability

### Logging
Use structured logging with MDC for traceability:

```java
@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    String serviceId = this.getClass().getSimpleName();
    MDC.put("serviceId", serviceId);
    MDC.put("paymentId", paymentRecord.getId().toString());
    
    try {
        LOG.info("Processing payment record: {}", paymentRecord);
        
        return processPayment(paymentRecord)
            .onItem().invoke(result -> {
                LOG.info("Payment processed successfully: {}", result);
                MDC.clear();
            })
            .onFailure().invoke(error -> {
                LOG.error("Payment processing failed", error);
                MDC.clear();
            });
    } finally {
        MDC.clear();
    }
}
```

### Metrics
Expose metrics for monitoring:

```java
@Inject
MetricRegistry metricRegistry;

private Timer processingTimer;
private Counter successCounter;
private Counter failureCounter;

@PostConstruct
void initializeMetrics() {
    processingTimer = metricRegistry.timer("payment.processing.duration");
    successCounter = metricRegistry.counter("payment.processing.success");
    failureCounter = metricRegistry.counter("payment.processing.failure");
}

@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    Timer.Context timerContext = processingTimer.time();
    
    return processPayment(paymentRecord)
        .onItem().invoke(result -> {
            timerContext.stop();
            successCounter.inc();
        })
        .onFailure().invoke(error -> {
            timerContext.stop();
            failureCounter.inc();
        });
}
```

## Best Practices

### Design Principles
1. **Single Responsibility**: Each service should have one clear purpose
2. **Statelessness**: Keep services stateless when possible
3. **Immutability**: Use immutable objects where feasible
4. **Fail Fast**: Validate inputs early and fail fast
5. **Graceful Degradation**: Handle failures gracefully

### Performance Considerations
1. **Use Virtual Threads**: Leverage virtual threads for I/O-bound operations
2. **Avoid Blocking Operations**: Use reactive patterns throughout
3. **Efficient Memory Usage**: Be mindful of memory allocation in hot paths
4. **Batch Processing**: Consider batching for high-volume scenarios

### Security
1. **Input Validation**: Validate all inputs thoroughly
2. **Sanitization**: Sanitize data before processing
3. **Authentication**: Implement proper authentication for service-to-service communication
4. **Authorization**: Check permissions where appropriate

### Maintainability
1. **Clear Naming**: Use descriptive names for classes, methods, and variables
2. **Documentation**: Document complex logic and edge cases
3. **Testing**: Write comprehensive tests for all scenarios
4. **Configuration**: Make behavior configurable where appropriate