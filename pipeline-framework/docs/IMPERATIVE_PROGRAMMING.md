# Using Collection-Based and Future-Based Programming with Pipeline Framework

## Overview

This guide explains how to use collection-based and Future-based programming (blocking operations) with the Pipeline Framework while still benefiting from its reactive capabilities and auto-persistence features.

## When to Use Collection-Based and Future-Based Steps

Collection-based and Future-based steps are appropriate when:

1. You're not familiar with reactive programming concepts
2. You need to integrate with blocking libraries or services
3. Your organization prefers working with standard Java collections and Futures
4. You don't require high throughput or are okay with lower performance
5. You're using databases like MySQL that may not have optimal reactive support

## Step Interfaces

The Pipeline Framework provides several step interfaces for different programming styles:

### 1. StepOneToOne<I, O> - Synchronous 1:1 Transformation
The standard interface that already supports blocking operations well.

### 2. StepOneToManyCollection<I, O> - Collection-based 1:N Transformation
Works with `List<O>` instead of `Multi<O>` for developers who prefer standard Java collections.

### 3. StepManyToManyCollection - Collection-based N:M Transformation
Works with `List` collections instead of `Multi` streams.

### 4. StepCompletableFuture<I, O> - Future-based Asynchronous 1:1 Transformation
Works with `CompletableFuture<O>` instead of `Uni<O>` for developers who prefer standard Java Futures.

## How It Works Internally

1. The PipelineRunner wraps your blocking `apply()` method in a reactive context
2. It executes the blocking operation on a virtual thread to prevent platform thread blocking
3. The result is merged back into the reactive stream
4. Auto-persistence works as a side effect without blocking the main flow

## MySQL Configuration

To use MySQL instead of PostgreSQL:

### 1. Update Dependencies

Replace PostgreSQL dependencies in your `pom.xml`:

```xml
<!-- Replace these PostgreSQL dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-reactive-pg-client</artifactId>
</dependency>

<!-- With MySQL dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-reactive-mysql-client</artifactId>
</dependency>
```

### 2. Update Configuration

In your `application.properties`:

```properties
# Change from PostgreSQL to MySQL
quarkus.datasource.db-kind=mysql
quarkus.datasource.reactive.url=mysql://localhost:3306/mydb
```

## Performance Considerations

### Benefits of Imperative Approach

1. **Simplicity**: Easier to understand and implement for developers unfamiliar with reactive programming
2. **Compatibility**: Works well with existing blocking libraries and services
3. **Debugging**: Easier to debug with standard tools and techniques

### Trade-offs

1. **Performance**: Lower throughput compared to fully reactive implementations
2. **Resource Usage**: Uses more threads than pure reactive approaches
3. **Scalability**: May not scale as well under high load

## Best Practices

1. **Use Virtual Threads**: Enable virtual threads for better resource utilization:
   ```java
   validateStep.liveConfig().overrides().runWithVirtualThreads(true);
   ```

2. **Entity Annotations**: Ensure your entities are annotated with `@Entity` for auto-persistence:
   ```java
   @Entity
   public class PaymentRecord {
       // ... fields and methods
   }
   ```

3. **Error Handling**: Implement proper error handling in your imperative steps:
   ```java
   @Override
   public TestPaymentEntity apply(TestPaymentEntity payment) {
       try {
           // Your processing logic
           return processPayment(payment);
       } catch (Exception e) {
           // Handle errors appropriately
           payment.setStatus("FAILED");
           return payment;
       }
   }
   ```

4. **Configuration**: Use the configuration system to control behavior:
   ```java
   validateStep.liveConfig().overrides()
       .autoPersist(true)
       .retryLimit(3)
       .retryWait(Duration.ofSeconds(1));
   ```

## Example Pipeline

Here's a complete example of a pipeline using imperative steps:

```java
// Create entities
TestPaymentEntity payment1 = new TestPaymentEntity("John Doe", new BigDecimal("100.00"));
TestPaymentEntity payment2 = new TestPaymentEntity("Jane Smith", new BigDecimal("250.50"));

Multi<TestPaymentEntity> input = Multi.createFrom().items(payment1, payment2);

// Create imperative steps
ValidatePaymentStep validateStep = new ValidatePaymentStep();
validateStep.liveConfig().overrides().autoPersist(true);

EnrichPaymentStep enrichStep = new EnrichPaymentStep();
enrichStep.liveConfig().overrides().autoPersist(true);

// Run pipeline
PipelineRunner pipelineRunner = new PipelineRunner();
Multi<Object> result = pipelineRunner.run(input, List.of(validateStep, enrichStep));
```

This approach gives you the benefits of the Pipeline Framework (configuration, resilience, observability) while allowing you to write simple imperative code.