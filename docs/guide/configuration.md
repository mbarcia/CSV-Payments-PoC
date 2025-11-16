# Configuration

The Pipeline Framework uses a Quarkus-based configuration system for managing step configuration properties. This system provides both global defaults and per-step overrides for various configuration properties.

## Configuration Properties

### Global Configuration

All configuration properties are accessed under the `pipeline` prefix:

```properties
# Global defaults for all steps
pipeline.retry-limit=10
pipeline.retry-wait-ms=3000
pipeline.debug=true
pipeline.parallel=false
pipeline.recover-on-failure=true
pipeline.run-on-virtual-threads=false
pipeline.max-backoff=60000
pipeline.jitter=true
pipeline.backpressure-buffer-capacity=2048
pipeline.backpressure-strategy=DROP

# gRPC clients
quarkus.grpc.clients.process-payment.host=localhost
quarkus.grpc.clients.process-payment.port=8080
```

### Per-Step Configuration

You can override configuration for specific steps using the fully qualified class name:

```properties
# Override for specific step
pipeline."org.example.MyStep".retry-limit=5
pipeline."org.example.MyStep".debug=false
pipeline."org.example.MyStep".parallel=true
pipeline."org.example.MyStep".order=100
pipeline."org.example.MyStep".run-on-virtual-threads=true
```

### Step Definition Configuration

Steps are still defined using the `pipeline.steps` configuration:

```properties
# Define and configure steps
pipeline.steps.my-step.class=org.example.MyStep
pipeline.steps.my-step.order=100
pipeline.steps.my-step.type=ONE_TO_ONE
```

The `order` property determines the execution order of the steps in the pipeline, and `run-on-virtual-threads` specifies whether the step should run on virtual threads.

## Parallel Processing Configuration

For any step type, you can configure parallel processing to process multiple items from the same stream concurrently. This can be set globally or per-step:

```properties
# Global setting
pipeline.parallel=true

# Or per-step override
pipeline."org.example.MyStep".parallel=true
```

For individual steps, you can also configure this using the `@PipelineStep` annotation:

```java
@PipelineStep(
    parallel = true          // Enable parallel processing for this step
)
```

### Parallel Processing Parameters

- **`parallel`**: Controls whether to enable parallel processing for this step. Default is `false` (sequential processing). When set to `true`, the service can process multiple items from the same input stream concurrently, dramatically improving throughput when some items take longer than others. For example, in a payment processing service, if one payment takes 10 seconds but others take 1 second, setting `parallel = true` allows the fast payments to complete without waiting for the slow ones.

### Choosing the Right Parallel Strategy

For maximum performance when order doesn't matter, use:
- `pipeline.parallel=true` or `pipeline."FQCN".parallel=true`
- `parallel = true` in the `@PipelineStep` annotation

For strict sequential processing, use the defaults:
- `pipeline.parallel=false` (default, sequential processing)
- `parallel = false` in the `@PipelineStep` annotation (default)

## Avoid breaking parallelism in the pipeline

*Important*

If any previous step uses `parallel = false` (the default), the pipeline will serialize the stream at that point.

Downstream the pipeline cannot "rewind" concurrency — the upstream won't push items faster than it finished
sequentially.

Hence, the more "downstream" you can push the `parallel = false` moment, the faster the pipeline will process streams.

## Virtual Thread Configuration

Virtual threads can be enabled globally or per-step:

```properties
# Global setting for virtual threads
pipeline.run-on-virtual-threads=true

# Or per-step override
pipeline."org.example.MyStep".run-on-virtual-threads=true
```

### Virtual Thread Parameter

- **`run-on-virtual-threads`**: Controls whether a step should run using virtual threads. When enabled, the framework will automatically add `@RunOnVirtualThread` annotations to generated gRPC services and REST resources. This is particularly useful for I/O-bound operations.

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
- **Parallelism**: Processing-level parallelism with `parallel` parameter
- **Best for**: Aggregation operations that can benefit from concurrent processing

```java
@PipelineStep(
    stepType = StepManyToOne.class,
    parallel = true  // Process with concurrent capabilities
)
public class ProcessCsvPaymentsOutputFileReactiveService
    implements ReactiveStreamingClientService<PaymentOutput, CsvPaymentsOutputFile> {

    @Override
    public Uni<CsvPaymentsOutputFile> process(Multi<PaymentOutput> paymentOutputList) {
        // Process all payment outputs to generate a single CSV file
        // With parallel=true, different processing tasks can be executed concurrently
        return writeToFile(paymentOutputList);
    }
}
```

**Important Consideration for File Operations**: When using StepManyToOne for file writing operations (like CSV generation), ensure all related records are available before starting the write operation. Some file writing libraries like OpenCSV may truncate files if data is provided in chunks. When implementing file processing operations:
- Collect all related records before starting the file writing operation
- Consider the trade-offs between memory usage (storing more records) versus correctness (avoiding file truncation)
- For file operations that require all related data to be available, ensure your implementation collects all necessary data before processing

**Approach for Complete File Processing**: It may be more appropriate to collect all related records before processing them. This ensures that file writing operations receive all records for a file at once, avoiding truncation issues:

```java
@Override
public Multi<CsvPaymentsOutputFile> process(Multi<PaymentOutput> paymentOutputMulti) {
    // Collect all elements before processing to ensure all records
    // for a file are processed together, preventing file truncation issues
    return paymentOutputMulti
        .collect()
        .asList()
        .onItem()
        .transformToMulti(paymentOutputs -> {
            // Group the collected list by input file path
            java.util.Map<java.nio.file.Path, java.util.List<PaymentOutput>> groupedOutputs =
                paymentOutputs.stream()
                    .collect(java.util.stream.Collectors.groupingBy(paymentOutput -> {
                        // Extract the input file path to group related records
                        PaymentStatus paymentStatus = paymentOutput.getPaymentStatus();
                        AckPaymentSent ackPaymentSent = paymentStatus.getAckPaymentSent();
                        PaymentRecord paymentRecord = ackPaymentSent.getPaymentRecord();
                        return paymentRecord.getCsvPaymentsInputFilePath().getFileName();
                    }));

            // Process each group of related records together
            return Multi.createFrom().iterable(groupedOutputs.entrySet())
                .flatMap(entry -> {
                    // Process all records in the group together
                    java.util.List<PaymentOutput> outputsForFile = entry.getValue();
                    // Write all records to the same file in a single operation to prevent truncation
                    return writeToFile(outputsForFile, entry.getKey());
                });
        });
}
```

### 4. Many-to-Many (N→N) - Multiple Inputs to Multiple Outputs
- **Use case**: Transform a stream of items where each may produce multiple outputs
- **Example**: Filter and transform a stream of records
- **Parallelism**: Item-level parallelism available

## @PipelineStep Annotation

The `@PipelineStep` annotation now contains only essential build-time properties (runtime configuration properties have been moved to Quarkus configuration):

- `inputType`, `outputType` - Type information for code generation
- `inputGrpcType`, `outputGrpcType` - gRPC type information
- `grpcStub`, `grpcImpl` - gRPC classes
- `inboundMapper`, `outboundMapper` - Mapper classes
- `stepType` - Step type class
- `backendType` - Backend adapter type
- `grpcClient` - GPRC client name
- `grpcEnabled` - Whether to enable gRPC generation
- `restEnabled` - Whether to enable REST generation
- `grpcServiceBaseClass` - GPRC service base class
- `local` - Whether step is local to the runner

Runtime configuration properties like `order` and `runOnVirtualThreads` have been moved from the annotation to Quarkus configuration system for better flexibility and management. These properties can now be configured using the following properties:

```properties
# Configure the execution order for a step
pipeline.steps.my-step.order=100

# Configure virtual threads for a step
pipeline.steps.my-step.run-on-virtual-threads=true
```

## Configuration Simplification

The configuration system has been simplified to use a single interface (`PipelineStepConfig`) with:

1. Global defaults through `config()` method
2. Per-step overrides through the `named()` map

This approach eliminates redundant configuration classes while maintaining flexibility.

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

## Performance Optimization Guidelines

### For Item-Level Processing (1→1 Steps):
1. **Set `parallel = true`** to enable concurrent processing of multiple input items
2. **Monitor system resources** under load to determine optimal concurrency level

### For Aggregation Processing (N→1 Steps):
1. **Use `parallel = true`** to enable concurrent processing when beneficial

### Virtual Thread Optimization:
1. **Enable `run-on-virtual-threads=true`** for I/O-bound operations
2. **Monitor thread utilization** to ensure virtual threads are providing benefits
3. **Use for operations involving blocking I/O** rather than CPU-intensive operations

### Monitoring and Tuning:
- Start with conservative parallelism values and increase gradually
- Monitor system resource usage (CPU, memory, network) under load
- Adjust `parallel` setting based on observed performance
- Consider the downstream service capacity limits when setting parallelism values
- Enable virtual threads for I/O-bound operations to improve throughput