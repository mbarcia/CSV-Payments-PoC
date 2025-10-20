# Configuration

Configure pipeline behavior through application properties and environment-specific profiles.

## Application Properties

Configure pipeline behavior through application properties:

```properties
# application.properties
# Pipeline configuration
pipeline.retry-limit=3
pipeline.debug=false
pipeline.auto-persist=true

# gRPC clients
quarkus.grpc.clients.process-payment.host=localhost
quarkus.grpc.clients.process-payment.port=8080
```

## Parallel Processing Configuration

For any step type, you can configure parallel processing to process multiple items from the same stream concurrently using the `@PipelineStep` annotation:

```java
@PipelineStep(
    parallel = true          // Enable parallel processing for this step
)
```

### Parallel Processing Parameters

- **`parallel`**: Controls whether to enable parallel processing for this step. Default is `false` (sequential processing). When set to `true`, the service can process multiple items from the same input stream concurrently, dramatically improving throughput when some items take longer than others. For example, in a payment processing service, if one payment takes 10 seconds but others take 1 second, setting `parallel = true` allows the fast payments to complete without waiting for the slow ones.

### Choosing the Right Parallel Strategy

For maximum performance when order doesn't matter, use:
- `parallel = true` 

For strict sequential processing, use the defaults:
- `parallel = false` (default, sequential processing)

## Avoid breaking parallelism in the pipeline

*Important*

If any previous step uses `parallel = false` (the default), the pipeline will serialize the stream at that point. 

Downstream the pipeline cannot “rewind” concurrency — the upstream won’t push items faster than it finished 
sequentially.

Hence, the more "downstream" you can push the `parallel = false` moment, the faster the pipeline will process streams.

## Batch Processing Configuration

For `StepManyToOne` steps, you can configure batch processing behavior through the `@PipelineStep` annotation:

```java
@PipelineStep(
    batchSize = 50,           // Collect up to 50 items before processing
    batchTimeoutMs = 5000,    // Or process after 5 seconds, whichever comes first
    parallel = true           // Process batches concurrently
)
```

### Batch Processing Parameters

- **`batchSize`**: The maximum number of items to collect in a batch before processing begins. For related records (such as all PaymentOutput records from the same CSV file), set this to a value larger than the expected number of related items to ensure they are processed together.

- **`batchTimeoutMs`**: The maximum time (in milliseconds) to wait for additional items to accumulate in a batch. Even if the batch size hasn't been reached, processing will begin after this timeout expires.

- **`parallel`**: When `true`, this enables concurrent processing of batches:
  - **`true`**: Process batches concurrently, maximizing throughput. This strategy starts multiple batches at the same time, which can significantly improve performance but does not preserve the order of batch completion.
  - **`false`**: Process batches sequentially, preserving order. This strategy waits for each batch to complete before starting the next one, ensuring deterministic processing order.

### Choosing the Right Strategy

Choose `parallel = true` when you need maximum performance and the order of batch processing is not important. Choose `parallel = false` when you need to maintain predictable ordering of batch results.

## Parallel Processing Configuration

For any step type, you can configure parallel processing to process multiple items from the same stream concurrently using the `@PipelineStep` annotation:

```java
@PipelineStep(
    parallel = true          // Enable parallel processing for this step
)
```

### Parallel Processing Parameters

- **`parallel`**: Controls whether to enable parallel processing for this step. Default is `false` (sequential processing). When set to `true`, the service can process multiple items simultaneously, dramatically improving throughput when some items take longer than others. For example, in a payment processing service, if one payment takes 10 seconds but others take 1 second, setting `parallel = true` allows the fast payments to complete without waiting for the slow ones.

## Understanding Pipeline Step Cardinalities

The Pipeline Framework supports four different cardinality types that determine how input and output streams are processed:

### 1. One-to-One (1→1) - Single Input to Single Output
- **Use case**: Transform each individual item into another item
- **Example**: Convert a payment record into a payment status
- **Parallelism**: Item-level parallelism with `parallel` parameter
- **Best for**: Individual processing operations that can benefit from concurrent execution

```java
@PipelineStep(
    stepType = StepOneToOne.class,
    parallel = true  // Process multiple payment records concurrently
)
public class ProcessPaymentService implements StepOneToOne<PaymentRecord, PaymentStatus> {
    @Override
    public Uni<PaymentStatus> applyOneToOne(PaymentRecord input) {
        // Each payment record is processed independently
        // With parallel=true, multiple records can be processed concurrently
        return sendPayment(input);
    }
}
```

### 2. One-to-Many (1→N) - Single Input to Multiple Outputs
- **Use case**: Expand a single item into multiple related items
- **Example**: Split a batch job into individual tasks
- **Parallelism**: Item-level parallelism with `parallel` parameter

### 3. Many-to-One (N→1) - Multiple Inputs to Single Output
- **Use case**: Aggregate multiple related items into a single result
- **Example**: Collect payment outputs and write them to a single CSV file
- **Parallelism**: Batch-level parallelism with `parallel` parameter
- **Best for**: Batch aggregation operations that can benefit from concurrent batch processing

```java
@PipelineStep(
    stepType = StepManyToOne.class,
    batchSize = 50,  // Collect up to 50 payment outputs before writing to CSV
    batchTimeoutMs = 5000,  // Or write after 5 seconds even if batch isn't full
    parallel = true  // Process different batches concurrently
)
public class ProcessCsvPaymentsOutputFileReactiveService 
    implements ReactiveStreamingClientService<PaymentOutput, CsvPaymentsOutputFile> {
    
    @Override
    public Uni<CsvPaymentsOutputFile> process(Multi<PaymentOutput> paymentOutputList) {
        // All payment outputs in a batch are processed together
        // With parallel=true, different batches can be processed concurrently
        return writeToFile(paymentOutputList);
    }
}
```

**Important Consideration for File Operations**: When using StepManyToOne for file writing operations (like CSV generation), be aware that the batching mechanism can cause issues with libraries like OpenCSV. If a batch does not contain all records related to a single output file, the file writing operation may truncate the file. When configuring `batchSize` and `batchTimeoutMs` for file operations:
- Ensure values are set to capture ALL related records for a single output file
- Consider the trade-offs between memory usage (larger batches) versus correctness (avoiding file truncation)
- For file operations that require all related data to be available, carefully tune the batching parameters to match your data patterns

### 4. Many-to-Many (N→N) - Multiple Inputs to Multiple Outputs
- **Use case**: Transform a stream of items where each may produce multiple outputs
- **Example**: Filter and transform a stream of records
- **Parallelism**: Item-level and batch-level parallelism available

## Practical Example: CSV Payments Pipeline Optimization

The CSV payments pipeline demonstrates how to use parallelism for optimal performance:

### Scenario: Processing Multiple Payment Records Concurrently
1. **CSV file contains** multiple payment records
2. **Each record** needs to be sent to a third-party payment provider
3. **Some records** take longer to process than others
4. **Goal**: Don't let slow records block fast records

### Solution: Use Parallel Processing in SendPayment Step
```java
@PipelineStep(
    order = 2,
    inputType = PaymentRecord.class,
    outputType = AckPaymentSent.class,
    stepType = StepOneToOne.class,
    parallel = true  // Send multiple payment records concurrently
)
public class SendPaymentRecordReactiveService 
    implements StepOneToOne<PaymentRecord, AckPaymentSent> {
    
    @Override
    public Uni<AckPaymentSent> applyOneToOne(PaymentRecord paymentRecord) {
        // This method is called for each payment record
        // With parallel=true, multiple records can be processed simultaneously
        // Fast records complete quickly without waiting for slow ones
        return sendToThirdPartyPaymentProvider(paymentRecord);
    }
}
```

### Scenario: Processing Multiple CSV Files Concurrently
1. **Multiple CSV files** arrive for processing
2. **Each file** contains related payment records that should be processed together
3. **Goal**: Don't wait for one file to finish before starting the next

### Solution: Use Batch-Level Parallelism in Output Processing
```java
@PipelineStep(
    order = 6,
    stepType = StepManyToOne.class,
    batchSize = 50,  // Process up to 50 payment outputs together in one batch
    batchTimeoutMs = 10000,  // Wait 10 seconds to collect related records
    parallel = true  // Process different batches concurrently
)
public class ProcessCsvPaymentsOutputFileReactiveService 
    implements ReactiveStreamingClientService<PaymentOutput, CsvPaymentsOutputFile> {
    
    @Override
    public Uni<CsvPaymentsOutputFile> process(Multi<PaymentOutput> paymentOutputList) {
        // All payment outputs from the same CSV file are processed together in a batch
        // With parallel=true, different batches (from different files) 
        // can be processed concurrently
        return writeToFile(paymentOutputList);
    }
}
```

## Performance Optimization Guidelines

### For Item-Level Processing (1→1 Steps):
1. **Set `parallel = true`** to enable concurrent processing of multiple input items
2. **Monitor system resources** under load to determine optimal concurrency level

### For Batch Processing (N→1 Steps):
1. **Set `batchSize`** to match typical group sizes (e.g., CSV file record counts)
2. **Set `batchTimeoutMs`** to prevent indefinite waiting for batch completion
3. **Use `parallel = true`** to process different batches concurrently

### Monitoring and Tuning:
- Start with conservative parallelism values and increase gradually
- Monitor system resource usage (CPU, memory, network) under load
- Adjust `parallel` setting based on observed performance
- Consider the downstream service capacity limits when setting parallelism values