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

This project now consists of Maven submodules, each module containing a microservice that runs independently from the others.

The repository is organized as follows

# TODO

## Usage Instructions

# TODO

### Prerequisites

- Java 21
- Maven 3.6+
- Quarkus 3.x

### Installation

1. Clone the repository:
   ```
   git clone <repository-url>
   ```

# TODO

### Running the CLI application

# TODO

### Configuration

# TODO

### Testing

To run the tests, execute:

```
mvn -f pom.xml test
```

### Troubleshooting

1. Issue: Application fails to start
   - Check if the Java version is correct (Java 21 required)
   - Ensure all dependencies are properly downloaded (run `mvn -f pom.xml dependency:resolve`)
   - Verify the `application.properties` configurations

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