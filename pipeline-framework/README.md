# Pipeline Framework - New Features

## Many-to-One Steps

The pipeline framework now supports many-to-one steps, which allow you to process batches of inputs and produce a single output.

### Interface

```java
public interface StepManyToOne<I, O> extends StepBase {
    Uni<O> applyBatch(List<I> inputs);
    
    default int batchSize() { return 10; }
    default long batchTimeoutMs() { return 1000; }
}
```

### Usage Example

```java
public class PaymentAggregationStep implements StepManyToOne<PaymentRecord, PaymentSummary> {
    @Override
    public Uni<PaymentSummary> applyBatch(List<PaymentRecord> payments) {
        // Aggregate payments into a summary
        BigDecimal totalAmount = payments.stream()
            .map(PaymentRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        PaymentSummary summary = new PaymentSummary();
        summary.setTotalAmount(totalAmount);
        summary.setTotalCount(payments.size());
        summary.setProcessedAt(LocalDateTime.now());
        
        return Uni.createFrom().item(summary);
    }
    
    @Override
    public int batchSize() {
        return 50; // Process in batches of 50
    }
}
```

### Imperative Version

For blocking operations, you can use the imperative version:

```java
public interface StepManyToOneImperative<I, O> extends StepBase {
    O applyBatchList(List<I> inputs);
}
```

## Flexible Database Persistence

The framework now provides more flexible database persistence options while maintaining the zero-configuration experience.

### Database Configuration

You can now configure your database choice through configuration properties:

```properties
# application.properties
pipeline.database.type=POSTGRESQL
pipeline.database.url=jdbc:postgresql://localhost:5432/mydb
pipeline.database.username=myuser
pipeline.database.password=mypassword
```

### Supported Databases

- PostgreSQL
- MySQL
- H2 (default for zero-configuration)
- SQLite
- Oracle
- SQL Server

### Zero-Configuration Experience

If no database is explicitly configured, the framework will automatically detect what's available in the classpath and use it:

1. PostgreSQL (if `org.postgresql.Driver` is present)
2. MySQL (if `com.mysql.cj.jdbc.Driver` is present)
3. H2 (default fallback)

### Custom Database Configuration

You can also provide your own database configuration by implementing the `DatabaseConfig` interface:

```java
@ApplicationScoped
public class CustomDatabaseConfig implements DatabaseConfig {
    @Override
    public DatabaseType type() {
        return DatabaseType.POSTGRESQL;
    }
    
    @Override
    public String jdbcUrl() {
        return "jdbc:postgresql://localhost:5432/mydb";
    }
    
    @Override
    public String username() {
        return "myuser";
    }
    
    @Override
    public String password() {
        return "mypassword";
    }
}
```