# Data Persistence Service

This service is responsible for persisting payment records and acknowledgment messages in the CSV Payments Processing System.

## Overview

The Data Persistence Service provides reactive persistence capabilities for:
- Payment records processed from CSV files
- Acknowledgment messages received from the payment provider
- CSV input and output files
- Payment outputs for processed payments
- Payment status information
- CSV folder structures

It implements a clean architecture with repository patterns and reactive programming principles using Mutiny.

## Key Components

### Services

1. **PersistReactiveService<T>** - Generic interface for persistence operations
   - Defines the contract for persisting entities
   - Provides default implementation with transaction management and logging

2. **PersistPaymentRecordReactiveService** - Implementation for PaymentRecord entities
   - Persists payment records to the database

3. **PersistAckPaymentSentReactiveService** - Implementation for AckPaymentSent entities
   - Persists payment acknowledgment messages to the database

4. **PersistCsvPaymentsInputFileReactiveService** - Implementation for CsvPaymentsInputFile entities
   - Persists CSV input file metadata to the database

5. **PersistCsvPaymentsOutputFileReactiveService** - Implementation for CsvPaymentsOutputFile entities
   - Persists CSV output file metadata to the database

6. **PersistPaymentOutputReactiveService** - Implementation for PaymentOutput entities
   - Persists payment output records to the database

7. **PersistPaymentStatusReactiveService** - Implementation for PaymentStatus entities
   - Persists payment status information to the database

8. **PersistCsvFolderReactiveService** - Implementation for CsvFolder entities
   - Persists CSV folder metadata to the database

### Repository Pattern

The service uses a repository pattern to abstract data access:
- **PanacheRepository<T>** - Quarkus Hibernate Panache repository for reactive persistence operations

## Technology Stack

- **Java 21** with preview features enabled
- **Quarkus** framework for reactive programming
- **Hibernate Reactive** with Panache for database operations
- **PostgreSQL** as the database
- **Mutiny** for reactive programming
- **JUnit 5** for testing
- **Mockito** for mocking dependencies in tests

## Building the Project

To build the project, run:

```bash
mvn clean install
```

## Running Tests

To run the unit tests:

```bash
mvn test
```

The tests are pure JUnit 5 tests without Quarkus integration tests, ensuring fast execution and easy debugging.

## Building a Native Image

To build a native image using GraalVM:

```bash
mvn clean package -Pnative
```

## Running the Service

### JVM Mode

```bash
java -jar target/data-persistence-svc-1.0.0.jar
```

### Native Mode

```bash
./target/data-persistence-svc-1.0.0-runner
```

## Configuration

The service can be configured using environment variables or application.properties:

- `quarkus.datasource.reactive.url` - Database connection URL
- `quarkus.datasource.username` - Database username
- `quarkus.datasource.password` - Database password

## API Endpoints

This service is designed to work as part of a gRPC microservices architecture and doesn't expose REST endpoints directly.

The following gRPC services are available:

1. **PersistPaymentRecordService** - Persists PaymentRecord entities
2. **PersistAckPaymentSentService** - Persists AckPaymentSent entities
3. **PersistCsvPaymentsInputFileService** - Persists CsvPaymentsInputFile entities
4. **PersistCsvPaymentsOutputFileService** - Persists CsvPaymentsOutputFile entities
5. **PersistPaymentOutputService** - Persists PaymentOutput entities
6. **PersistPaymentStatusService** - Persists PaymentStatus entities
7. **PersistCsvFolderService** - Persists CsvFolder entities

## Testing Approach

The service follows a comprehensive testing approach:
- **Pure Unit Tests**: Using JUnit 5 and Mockito without Quarkus integration
- **100% Coverage**: All methods and lines are covered by tests
- **Repository Mocking**: Database operations are mocked to ensure true unit testing
- **Static Method Mocking**: SLF4J and MDC static methods are mocked for complete isolation

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a pull request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.