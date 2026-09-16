# Idempotent Payment / Wallet Event Processor

A Spring Boot backend service that processes payment wallet transactions safely under **concurrent requests and network retries**.

The application is designed around an internal transaction ledger where duplicate payment webhook requests can arrive concurrently. The implementation guarantees that the same `transactionId` is processed only once and prevents wallet balances from becoming negative during simultaneous debit requests.

## Tech Stack

* **Java 17**
* **Spring Boot**
* **Spring Data JPA**
* **H2 In-Memory Database**
* **JUnit 5**
* **Mockito**
* **Maven**

## Problem Statement

Payment gateways may retry webhook requests because of network failures or timeouts. As a result, the same transaction can reach the backend multiple times at nearly the same time.

For example, three requests may arrive concurrently with the same:

```json
{
  "transactionId": "UUID",
  "userId": "UUID",
  "amount": 250.00,
  "type": "DEBIT"
}
```

The system must ensure that:

* The transaction is processed only once.
* The wallet balance is deducted only once.
* Concurrent debit requests cannot produce a negative balance.
* The behavior is verified entirely through automated tests.
* No external database or infrastructure is required to run the test suite.

---

## API

### Process Transaction

```http
POST /api/v1/transactions/process
```

Example request:

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 250.00,
  "type": "DEBIT"
}
```

The service validates the transaction, obtains the required database lock, verifies idempotency, checks the wallet balance, and records the transaction.

---

## Concurrency and Idempotency

Two separate concurrency problems are handled by the application.

### 1. Duplicate Transaction Requests

The `transactionId` uniquely identifies a transaction.

For concurrent requests using the same `transactionId`, the application:

1. Starts a database transaction.
2. Acquires a database-level lock on the relevant wallet.
3. Checks whether the `transactionId` has already been processed.
4. Processes the transaction only when it has not already been recorded.
5. Persists the transaction and updated wallet balance.
6. Duplicate requests are rejected without deducting the wallet balance again.

A database uniqueness constraint on `transactionId` is also maintained as an additional layer of protection.

This prevents multiple concurrent webhook deliveries from creating multiple ledger entries for the same transaction.

### 2. Concurrent Debit Requests

The wallet is read using a **pessimistic database lock**.

This ensures that simultaneous debit operations against the same wallet are serialized at the database level.

For example, with a wallet balance of:

```text
₹500
```

and ten concurrent debit requests of:

```text
₹100
```

only five transactions can successfully debit the wallet.

The remaining requests fail because the wallet no longer has sufficient funds.

The final balance therefore remains:

```text
₹0
```

and never becomes negative.

---

## Automated Tests

The assignment is designed so that the evaluator can run the project directly from IntelliJ without Postman or an external database.

The test suite uses **JUnit 5** and an **H2 in-memory database**.

### Required Test 1 — Happy Path

```text
Processes a single valid debit transaction successfully.
```

Verifies that:

* A valid debit is processed.
* The wallet balance is updated correctly.
* The transaction is persisted successfully.

### Required Test 2 — Idempotency

```text
Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.
```

Verifies that three concurrent requests containing the same `transactionId` do not result in three balance deductions.

Exactly one transaction is processed while the duplicate requests do not update the balance again.

### Required Test 3 — Race Condition

```text
Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.
```

Verifies database-level concurrency control.

Expected result:

```text
Initial balance : ₹500
Requests         : 10
Amount/request   : ₹100
Successful       : 5
Failed           : 5
Final balance    : ₹0
```

---

## Zero-Configuration Testing

The project uses an **H2 in-memory database** specifically so that the complete test suite can run without:

* PostgreSQL
* MySQL
* Docker
* External database configuration
* Postman
* Additional infrastructure

The database is created automatically when the test suite starts and exists only for the test execution.

---

## Running the Project

### Prerequisites

* Java 17+
* Maven

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

### Run Tests

From the project root:

```bash
mvn test
```

Alternatively, open the project in IntelliJ IDEA and run the complete test suite.

The tests use H2 and therefore require no external database setup.

---

## Project Structure

The project follows a standard Spring Boot layered architecture:

```text
src
├── main
│   ├── java
│   │   └── ...
│   │       ├── controller
│   │       ├── service
│   │       ├── repository
│   │       ├── entity
│   │       ├── dto
│   │       └── exception
│   │
│   └── resources
│       └── application.properties
│
└── test
    ├── java
    │   └── ...
    └── resources
```

The exact package organization follows the implementation in the repository.

---

## Transaction Processing Flow

```text
HTTP Request
     |
     v
Transaction Controller
     |
     v
Transaction Service
     |
     |-- Start Transaction
     |
     |-- Lock Wallet (Database Lock)
     |
     |-- Check transactionId
     |
     |-- Validate balance
     |
     |-- Update wallet balance
     |
     |-- Save transaction
     |
     v
Database
```

For duplicate requests, the transaction ID check prevents the wallet from being deducted multiple times.

For concurrent debit requests, the database wallet lock ensures that each request works with the latest committed balance.

---

## Key Design Decisions

The major implementation decisions and the reasoning behind them are documented separately in:

```text
DECISIONS.md
```

The decision log specifically documents:

1. How the concurrency race condition was handled.
2. An incorrect/sub-optimal AI-assisted approach encountered during development and how it was corrected.

---

## Git Commit History

The project was developed incrementally with changes committed throughout the implementation rather than being submitted as a single final commit.

The repository therefore contains the development history of the assessment implementation.

---

## Assessment Requirements Covered

| Requirement                           | Implementation |
| ------------------------------------- | -------------- |
| Java 17+                              | Java 17        |
| Spring Boot                           | Yes            |
| H2 database                           | Yes            |
| Idempotent transaction processing     | Yes            |
| Duplicate transaction protection      | Yes            |
| Concurrent request handling           | Yes            |
| Database-level wallet locking         | Yes            |
| Negative balance prevention           | Yes            |
| JUnit 5                               | Yes            |
| `@DisplayName` tests                  | Yes            |
| 3 required concurrency/business tests | Yes            |
| Zero external setup for tests         | Yes            |
| Decision log                          | `DECISIONS.md` |
