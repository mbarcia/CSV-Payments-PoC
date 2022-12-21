![](https://github.com/mbarcia/CSV-Payments-PoC/workflows/tests/badge.svg)
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

### Running the application locally

The output is generated with the extension `csv.out` under `target/classes/csv/`

The data is persisted to an H2 database, using JPA/Hibernate. 

In order to inspect the db, set a debug breakpoint and use:
```
http://localhost:8080/h2-console
```

Driver Class: `org.h2.Driver`<br>
JDBC URL: `jdbc:h2:mem:poc`<br>
User Name: `sa`<br>
Password:<br>
