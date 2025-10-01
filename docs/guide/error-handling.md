# Concurrency, Error Handling, and DLQ

This guide explains the advanced features of The Pipeline Framework including concurrency control, error handling mechanisms, and dead letter queue (DLQ) support.

## Concurrency Control

The Pipeline Framework provides sophisticated concurrency controls to optimize performance while maintaining resource efficiency.

### Virtual Threads

The framework leverages Java 21's virtual threads for high-throughput, low-overhead concurrency:

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
    runWithVirtualThreads = true  // Enable virtual threads
)
@ApplicationScoped
public class ProcessPaymentService implements StepOneToOne<PaymentRecord, PaymentStatus> {
    
    @Override
    public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
        // This will run on a virtual thread if enabled
        return processPaymentWithExternalService(paymentRecord);
    }
    
    private Uni<PaymentStatus> processPaymentWithExternalService(PaymentRecord record) {
        // Virtual threads are ideal for I/O-bound operations like HTTP calls
        return webClient.post("/process-payment")
            .sendJson(record)
            .onItem().transform(response -> {
                // Processing response on virtual thread
                return convertResponseToPaymentStatus(response, record);
            });
    }
}
```

### Concurrency Limits

Control the maximum number of concurrent operations:

```java
@PipelineStep(
    // ... other configuration
    concurrency = 1000  // Limit to 1000 concurrent operations
)
```

Configuration can also be set globally or per-profile:

```properties
# application.properties
pipeline.concurrency=2000

# application-dev.properties  
pipeline.concurrency=100

# application-prod.properties
pipeline.concurrency=5000
```

### Backpressure Handling

The framework automatically handles backpressure through reactive streams with configurable strategies. You can control backpressure behavior through the `@PipelineStep` annotation:

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
    // Backpressure configuration
    backpressureBufferCapacity = 1024,           // Buffer capacity when using BUFFER strategy
    backpressureStrategy = "BUFFER"              // BUFFER, DROP, or ERROR strategy
)
```

The available overflow strategies are:

- **BUFFER** (default): Buffers items when the downstream consumer cannot keep up (using `onOverflow().buffer(capacity)`)
- **DROP**: Drops items when the downstream consumer cannot keep up (using `onOverflow().drop()`)

Note: In Mutiny 2.9.4, the explicit `onOverflow().fail()` method is not available. By default, Mutiny will signal an error when overflow occurs if no other overflow strategy is specified.

Programmatic configuration is also possible:

```java
@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    return externalService.process(paymentRecord)
        .onFailure().retry().withBackOff(Duration.ofMillis(100), Duration.ofSeconds(5))
        .atMost(3);
}

// The framework automatically applies the configured backpressure strategy
// based on the annotation or configuration settings.
```

### Relationship Between Concurrency and Buffer

Concurrency and buffer settings work together to control flow:

- **Concurrency** limits the number of simultaneous operations that can be processed
- **Buffer** controls how many items are queued when downstream operations are slower than upstream production

Best practices:
- Set concurrency based on system resources and external service limits
- Set buffer capacity based on expected load spikes and acceptable memory usage
- Monitor system performance to balance between throughput and resource utilization
- Consider using virtual threads when dealing with I/O-bound operations to improve concurrency efficiency

## Error Handling

The Pipeline Framework provides comprehensive error handling with multiple recovery strategies.

### Retry Mechanisms

Built-in retry with exponential backoff:

```java
@PipelineStep(
    // ... other configuration
    retryLimit = 5,           // Retry up to 5 times
    retryWait = "PT1S",       // Initial wait of 1 second
    maxBackoff = "PT30S",     // Maximum backoff of 30 seconds
    jitter = true             // Add randomization to prevent thundering herd
)
```

Programmatic retry configuration:

```java
@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    return processPayment(paymentRecord)
        .onFailure().retry()
        .withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(30))
        .withJitter(0.5)  // 50% jitter
        .atMost(5);       // Maximum 5 retries
}

private Uni<PaymentStatus> processPayment(PaymentRecord record) {
    return externalPaymentService.process(record)
        .onFailure(PaymentServiceException.class)
        .retry().withBackOff(Duration.ofMillis(500))
        .atMost(3);
}
```

### Circuit Breaker Pattern

Implement circuit breaker for external service calls:

```java
@Inject
CircuitBreaker circuitBreaker;

@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    return circuitBreaker.execute(
        () -> processPayment(paymentRecord),
        // Fallback method when circuit is open
        error -> handleCircuitBreakerFallback(paymentRecord, error)
    );
}

private Uni<PaymentStatus> handleCircuitBreakerFallback(PaymentRecord record, Throwable error) {
    LOG.warn("Circuit breaker triggered for payment: {}", record.getId(), error);
    
    // Return a default/fallback response
    return Uni.createFrom().item(
        PaymentStatus.builder()
            .paymentRecord(record)
            .status("CIRCUIT_BREAKER")
            .message("Service temporarily unavailable")
            .build()
    );
}
```

### Error Classification

Differentiate between recoverable and unrecoverable errors:

```java
@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    return processPayment(paymentRecord)
        .onFailure().recoverWithUni(error -> {
            if (isRecoverableError(error)) {
                return handleRecoverableError(paymentRecord, error);
            } else {
                return handleUnrecoverableError(paymentRecord, error);
            }
        });
}

private boolean isRecoverableError(Throwable error) {
    return error instanceof TimeoutException || 
           error instanceof NetworkException ||
           error instanceof ServiceUnavailableException;
}

private Uni<PaymentStatus> handleRecoverableError(PaymentRecord record, Throwable error) {
    LOG.warn("Recoverable error processing payment: {}", record.getId(), error);
    
    // Retry or send to DLQ
    if (shouldRetry(record)) {
        return Uni.createFrom().failure(error); // Will trigger retry
    } else {
        return deadLetter(record, error); // Send to DLQ
    }
}

private Uni<PaymentStatus> handleUnrecoverableError(PaymentRecord record, Throwable error) {
    LOG.error("Unrecoverable error processing payment: {}", record.getId(), error);
    
    // Send directly to DLQ
    return deadLetter(record, error);
}
```

## Dead Letter Queue (DLQ)

The DLQ mechanism captures failed items for later inspection and reprocessing.

### DLQ Configuration

Enable DLQ at the step level:

```java
@PipelineStep(
    // ... other configuration
    recoverOnFailure = true,  // Enable DLQ
    autoPersist = true        // Auto-persist items for DLQ
)
```

### Persistence Dependencies

When using `autoPersist = true`, you must include the necessary persistence dependencies in your `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-reactive-pg-client</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-reactive-panache</artifactId>
</dependency>
```

If you do not need persistence functionality, you can omit these dependencies and set `autoPersist = false` on all steps. This allows you to run pipeline services without database dependencies.

### Custom DLQ Implementation

Implement custom DLQ handling:

```java
@Override
public Uni<PaymentStatus> deadLetter(PaymentRecord paymentRecord, Throwable error) {
    LOG.warn("Sending failed payment record to dead letter queue: {}", paymentRecord.getId(), error);
    
    // Send to DLQ (database, message queue, file system, etc.)
    return persistToDeadLetterQueue(paymentRecord, error)
        .onItem().transform(v -> createDlqStatus(paymentRecord))
        .onFailure().recoverWithUni(failure -> {
            LOG.error("Failed to persist to DLQ", failure);
            // Fallback to error status
            return Uni.createFrom().item(createErrorStatus(paymentRecord, failure));
        });
}

private Uni<Void> persistToDeadLetterQueue(PaymentRecord record, Throwable error) {
    DeadLetterEntry entry = DeadLetterEntry.builder()
        .itemId(record.getId())
        .itemType("PaymentRecord")
        .errorMessage(error.getMessage())
        .errorStackTrace(getStackTraceAsString(error))
        .timestamp(Instant.now())
        .build();
    
    // Persist to database or message queue
    return deadLetterRepository.save(entry);
}

private PaymentStatus createDlqStatus(PaymentRecord record) {
    return PaymentStatus.builder()
        .paymentRecord(record)
        .status("DLQ")
        .message("Moved to dead letter queue after failed processing")
        .build();
}

private PaymentStatus createErrorStatus(PaymentRecord record, Throwable error) {
    return PaymentStatus.builder()
        .paymentRecord(record)
        .status("ERROR")
        .message("Failed to process and unable to persist to DLQ: " + error.getMessage())
        .build();
}
```

### DLQ Monitoring

Monitor DLQ for failed items:

```java
@ApplicationScoped
public class DeadLetterQueueMonitor {
    
    @Inject
    DeadLetterRepository deadLetterRepository;
    
    @Scheduled(every = "5m")  // Check every 5 minutes
    public void checkDeadLetterQueue() {
        deadLetterRepository.findUnprocessedEntries()
            .onItem().transformToMulti(entries -> Multi.createFrom().iterable(entries))
            .subscribe().with(this::processDeadLetterEntry);
    }
    
    private void processDeadLetterEntry(DeadLetterEntry entry) {
        LOG.info("Processing DLQ entry: {}", entry.getItemId());
        
        // Attempt reprocessing or notify administrators
        attemptReprocessing(entry)
            .onItem().invoke(success -> {
                if (success) {
                    // Mark as processed
                    deadLetterRepository.markAsProcessed(entry.getId());
                }
            })
            .subscribe().with(
                _ -> LOG.info("DLQ entry processed: {}", entry.getItemId()),
                error -> LOG.error("Error processing DLQ entry: {}", entry.getItemId(), error)
            );
    }
    
    private Uni<Boolean> attemptReprocessing(DeadLetterEntry entry) {
        // Logic to reprocess the failed item
        return Uni.createFrom().item(true);
    }
}
```

## Advanced Error Handling Patterns

### Error Context Preservation

Preserve error context for better debugging:

```java
@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    MDC.put("paymentId", paymentRecord.getId().toString());
    MDC.put("customerId", paymentRecord.getCustomerId());
    
    try {
        return processPayment(paymentRecord)
            .onItem().invoke(result -> {
                LOG.info("Payment processed successfully: {}", result.getStatus());
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

### Error Enrichment

Add contextual information to errors:

```java
private Uni<PaymentStatus> processPayment(PaymentRecord record) {
    return externalService.process(record)
        .onFailure().transform(error -> {
            // Enrich error with contextual information
            return new PaymentProcessingException(
                "Failed to process payment for customer: " + record.getCustomerId(),
                error,
                record.getId(),
                record.getCustomerId()
            );
        });
}
```

### Graceful Degradation

Implement graceful degradation for partial failures:

```java
@Override
public Uni<PaymentStatus> applyOneToOne(PaymentRecord paymentRecord) {
    return processPrimaryService(paymentRecord)
        .onFailure().recoverWithUni(error -> {
            LOG.warn("Primary service failed, falling back to secondary", error);
            return processSecondaryService(paymentRecord);
        })
        .onFailure().recoverWithUni(error -> {
            LOG.warn("Both services failed, using cached data", error);
            return processCachedData(paymentRecord);
        });
}
```

## Configuration Reference

### Global Configuration

```properties
# application.properties
# Retry configuration
pipeline.retry-limit=3
pipeline.retry-wait=PT500MS
pipeline.max-backoff=PT30S
pipeline.jitter=true

# Concurrency configuration
pipeline.concurrency=1000
pipeline.run-with-virtual-threads=true

# Error handling
pipeline.recover-on-failure=true
pipeline.auto-persist=true
pipeline.debug=false
```

### Profile-Specific Configuration

```properties
# application-dev.properties
pipeline.retry-limit=1
pipeline.retry-wait=PT100MS
pipeline.debug=true
pipeline.run-with-virtual-threads=false  # Disable in dev for easier debugging

# application-prod.properties
pipeline.retry-limit=5
pipeline.retry-wait=PT1S
pipeline.max-backoff=PT60S
pipeline.concurrency=5000
pipeline.run-with-virtual-threads=true
```

### Runtime Configuration

Modify configuration at runtime:

```java
@Inject
ConfigurableStep step;

public void updateStepConfiguration() {
    step.liveConfig()
        .retryLimit(10)
        .retryWait(Duration.ofSeconds(2))
        .concurrency(2000)
        .debug(true)
        .recoverOnFailure(true);
}
```

## Best Practices

### Concurrency

1. **Use Virtual Threads**: Enable for I/O-bound operations
2. **Set Appropriate Limits**: Don't overwhelm external systems
3. **Monitor Resource Usage**: Watch for bottlenecks and adjust accordingly
4. **Consider Batch Processing**: For high-volume scenarios

### Error Handling

1. **Classify Errors**: Differentiate recoverable from unrecoverable errors
2. **Implement Timeouts**: Prevent indefinite hanging operations
3. **Use Circuit Breakers**: Protect against cascading failures
4. **Log Meaningfully**: Include context and stack traces

### DLQ Management

1. **Regular Monitoring**: Check DLQ for failed items regularly
2. **Automated Retries**: Implement automated retry mechanisms
3. **Alerting**: Notify on DLQ accumulation
4. **Root Cause Analysis**: Investigate and fix recurring issues

### Performance

1. **Profile Regularly**: Monitor performance under load
2. **Optimize Hot Paths**: Focus on frequently executed code
3. **Use Efficient Mappers**: Optimize data conversion operations
4. **Cache Strategically**: Cache expensive operations when appropriate

The Pipeline Framework's concurrency, error handling, and DLQ features provide a robust foundation for building resilient, high-performance pipeline applications that can gracefully handle failures and scale efficiently.