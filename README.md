[![Workflow for CSV-Payments-PoC](https://github.com/mbarcia/CSV-Payments-PoC/actions/workflows/tests.yaml/badge.svg)](https://github.com/mbarcia/CSV-Payments-PoC/actions/workflows/tests.yaml)

# Motivation
Write a command-line client to process CSV files containing payments.

Each line needs to be processed via a test provider which is an external API.

This is not as simple as just invoking a single API on the provider and returning the results because the provider processes payments asynchronously, and it is your job to print the ultimate status after processing.

This is the API call params

| Field     | M   | Type    | Description                                       |
|-----------|-----|---------|---------------------------------------------------|
| msisdn    | M   | string  | Recipient phone number.                           |
| amount    | M   | decimal | Amount to pay.                                    |
| currency  | M   | string  | ISO-4217 currency code of amount.                 |
| reference | O   | string  | Client-supplied identifier for this payment.      |
| url       | O   | string  | URL to send callback request with payment result. |

and the response

| Field          | M   | Type    | Description                              |
|----------------|-----|---------|------------------------------------------|
| status         | M   | decimal | Status of request. See "Status" section. |
| message        | O   | string  | Additional information about the status. |
| conversationID | O   | string  | Identifier for this request.             |

1. Get a session token.
2. Request an action (payment, name lookup, etc.)
3. If the action is asynchronous, await callback requests from the provider.
   Callbacks may be enabled or disabled as needed.
4. If the action is asynchronous and callbacks are disabled or no callback was
   received in the expected amount of time, poll for results as needed.

The CSV output columns should be:

1. AMOUNT
2. CSV ID
3. CURRENCY
4. FEE
5. MESSAGE
6. RECIPIENT
7. REFERENCE
8. STATUS


# Getting Started

# CSV Payments Processing Application

This Quarkus CLI application processes CSV payment files, sends payments to a third-party processor, and generates output files.

The CSV Payments Processing Application is designed to handle the end-to-end process of reading CSV payment input files, processing payment records, interacting with a third-party payment processor, and generating output files. It provides a robust and efficient solution for managing payment data in a CSV format.

Key features include:
- Reading CSV payment input files from a specified folder
- Processing payment records and sending them to a third-party payment processor
- Handling immediate partial responses and final full responses from the payment processor
- Generating CSV payment output files based on processed records
- Parallel processing of payment records for improved performance
- Comprehensive logging and error handling

## Repository Structure

The repository is organized as follows:

- `src/main/java/com/example/poc/`: Contains the main application code
   - `CsvPaymentsApplication.java`: Main entry point of the application
   - `client/`: Client-related classes
   - `command/`: Command pattern implementations for various operations
   - `domain/`: Domain model classes
   - `repository/`: Data access layer interfaces
   - `service/`: Service layer classes implementing business logic
- `src/main/resources/`: Configuration files
   - `application.yaml`: Application configuration file
- `src/test/`: Test classes mirroring the main package structure
- `pom.xml`: Maven project configuration file

Key integration points:
- `PaymentProvider` interface: Defines the contract for interacting with the third-party payment processor
- `OrchestratorService`: Orchestrates the entire payment processing workflow
- `CsvPaymentsApplication`: Processes the CLI parameters and calls the OrchestratorService

## Usage Instructions

### Prerequisites

- Java 21
- Maven 3.6+
- Quarkus 3.x

### Installation

1. Clone the repository:
   ```
   git clone <repository-url>
   ```
2. Navigate to the project directory:
   ```
   cd csv-payments-processing
   ```
3. Build the project:
   ```
   mvn -f pom-monolith.xml clean install
   ```

### Running the CLI application

To run the application, use the following command:
#### Using Maven
```
# Run with Maven
mvn -f pom-monolith.xml quarkus:dev -Dquarkus.args="--csv-folder=csv/"

# Or for production build
mvn -f pom-monolith.xml package
java -jar target/quarkus-app/quarkus-run.jar -Dquarkus.args="--csv-folder=csv/"
```

#### Using the Quarkys CLI tool
```
# Install Quarkus CLI if you haven't
# curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force quarkus@quarkusio

# Run in dev mode
quarkus dev --args="--csv-folder=csv/"

# Build and run
quarkus build
java -jar target/quarkus-app/quarkus-run.jar --csv-folder=csv/
```

Use --csv-folder=folder-path with the path to the folder containing the CSV input files. If not specified, the application will default to the "csv/" folder.

The output is generated with the extension `csv.out` under `target/classes/csv/`

The data is persisted to an H2 database, using JPA/Hibernate.

In order to inspect the db, set a debug breakpoint and use:
```
http://localhost:8080/h2-console
```

Driver Class: `org.h2.Driver`
JDBC URL: `jdbc:h2:mem:poc`
Username: `sa`
Password:

### Configuration

The application can be configured using the `application.yaml` file located in the `src/main/resources/` directory. Key configuration options include:

- Database settings (URL, username, password)
- Logging levels
- Third-party payment processor API settings (if applicable)

### Testing

To run the tests, execute:

```
mvn -f pom-monolith.xml test
```

### Troubleshooting

1. Issue: Application fails to start
   - Check if the Java version is correct (Java 21 required)
   - Ensure all dependencies are properly downloaded (run `mvn -f pom-monolith.xml dependency:resolve`)
   - Verify the `application.yaml` configuration

2. Issue: CSV files not being processed
   - Confirm the correct folder path is provided as an argument
   - Check file permissions on the input folder
   - Verify CSV file format matches expected structure

For performance optimization:
- Monitor CPU and memory usage during processing
- Consider increasing the parallel processing threads if hardware resources allow
- Profile the application using tools like VisualVM to identify bottlenecks

## Data Flow

The CSV Payments Processing Application follows this high-level data flow:

1. The application reads CSV payment input files from a specified folder.
2. Each input file is processed, extracting individual payment records.
3. Payment records are sent to a third-party payment processor (mocked in this implementation).
4. The application receives and processes immediate partial responses from the payment processor.
5. Final full responses are processed for each payment.
6. Output files are generated based on the processed payment records.
7. The application provides console output for debugging purposes.

```
[Input Folder] -> [Read CSV Files] -> [Extract Payment Records] -> [Send to Payment Processor]
                                                                          |
                                                                          v
[Generate Output Files] <- [Process Final Responses] <- [Process Partial Responses]
```

Note: The application uses parallel processing to handle multiple payment records simultaneously, improving overall performance.