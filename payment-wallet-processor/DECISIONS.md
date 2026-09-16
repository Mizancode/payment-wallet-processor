# Decision Log

This document records the important implementation decisions made while developing the Idempotent Payment / Wallet Event Processor.

---

## 1. How did you handle the concurrency race condition?

### Problem

The application needs to process wallet debits safely when multiple requests arrive concurrently.

A typical race condition would look like this:

```text
Wallet balance = ₹500

Request A reads ₹500
Request B reads ₹500
Request C reads ₹500
...
```

If all requests read the same balance before any of them updates it, multiple requests could incorrectly succeed and the wallet could potentially become negative.

The same problem exists with duplicate webhook requests. Multiple requests containing the same `transactionId` may arrive at almost exactly the same time.

### Decision

The implementation uses **database-level pessimistic locking** on the wallet.

The transaction processing flow is intentionally ordered as:

```text
Start database transaction
        |
        v
Acquire wallet database lock
        |
        v
Check transactionId
        |
        v
Validate transaction/balance
        |
        v
Update wallet
        |
        v
Persist transaction
        |
        v
Commit
```

The important part is that the wallet is locked **before the idempotency check and balance update are performed**.

This means concurrent operations against the same wallet cannot simultaneously modify the wallet based on the same stale balance.

### Why lock the wallet first?

During development, an earlier approach performed the duplicate transaction check before acquiring the wallet lock.

That approach was not sufficient for concurrent requests because multiple requests could pass the duplicate check before any of them had committed the transaction.

The final implementation therefore acquires the wallet lock first and performs the transaction ID check within the same transaction.

This makes the critical section deterministic under concurrent requests.

### Example

For ten concurrent requests of ₹100 against a wallet containing ₹500:

```text
Initial balance = ₹500

Request 1 -> ₹400
Request 2 -> ₹300
Request 3 -> ₹200
Request 4 -> ₹100
Request 5 -> ₹0
Request 6 -> insufficient funds
Request 7 -> insufficient funds
Request 8 -> insufficient funds
Request 9 -> insufficient funds
Request 10 -> insufficient funds
```

The expected final state is:

```text
Successful requests = 5
Failed requests     = 5
Final balance       = ₹0
```

The database lock ensures that each request sees the correct balance after the previous transaction has completed.

### Additional protection

The transaction identifier also has a database uniqueness constraint.

The application-level idempotency check handles the normal duplicate-request flow, while the database constraint provides an additional integrity guarantee if concurrent operations attempt to persist the same transaction.

---

## 2. Where did the AI assistant give an incorrect or sub-optimal suggestion?

During development, an initial approach suggested checking whether the `transactionId` already existed **before acquiring the wallet lock**.

The simplified flow was:

```text
Check transactionId
        |
        v
Lock wallet
        |
        v
Update wallet
        |
        v
Save transaction
```

This looked correct for sequential requests, but it was not sufficient for concurrent duplicate requests.

### What happened?

When three identical requests were executed concurrently, all three requests could reach the duplicate check before one transaction had committed its transaction record.

Therefore, all three requests could initially observe that the transaction did not yet exist.

This demonstrated that the idempotency check could not be treated as an independent operation outside the critical section.

### Correction

The final implementation changed the ordering to:

```text
Lock wallet
        |
        v
Check transactionId
        |
        v
Process only if transaction is new
        |
        v
Update wallet
        |
        v
Persist transaction
```

The database uniqueness constraint was also retained as an additional safeguard.

### Testing-related issue

Another sub-optimal approach involved putting transaction management at the test level in a way that conflicted with the service transaction.

This resulted in a transaction-related failure such as:

```text
No active transaction
```

The solution was to let the service method manage the actual transaction boundary and to perform verification queries appropriately after the operation.

This reinforced an important design principle for this assessment:

> The production service should own the transaction boundary for the business operation, while the test should coordinate concurrent calls and verify the resulting state.

---

## 3. Why H2 was used

The assignment explicitly requires zero-configuration integration testing.

H2 was selected because it allows the entire test suite to execute without requiring an externally installed database.

This provides:

* Fast test execution
* Zero external configuration
* Easy IntelliJ execution
* Reproducible test runs
* Automatic database lifecycle during tests

This also matches the evaluator's requirement that the project be evaluated by running the test suite directly.

---

## 4. Why database-level locking was preferred

The concurrency requirement is a data-consistency requirement.

Synchronizing Java threads alone would not provide the same database-level guarantee if multiple application instances were running.

Using database-level locking places the critical consistency mechanism around the actual persisted wallet record.

The approach therefore protects the wallet balance at the persistence layer rather than relying only on an in-memory Java synchronization mechanism.

---

## 5. Why idempotency uses `transactionId`

The webhook payload provides a `transactionId` that identifies the payment transaction.

Using this identifier as the idempotency key allows repeated deliveries of the same webhook to be recognized as the same business transaction.

The system therefore does not treat every HTTP request as a new payment merely because it arrived as a separate request.

---

## 6. Verification

The implementation was verified using the required automated scenarios:

### Happy Path

```text
Processes a single valid debit transaction successfully.
```

### Idempotency

```text
Sends 3 identical transactionIDs simultaneously.
Ensures the balance is only deducted once.
```

### Race Condition

```text
Sends 10 concurrent debit requests of ₹100
for a wallet with a ₹500 balance.
Ensures the final balance is exactly ₹0
and 5 requests fail with insufficient funds.
```

All three required tests pass successfully.

---

## Final Implementation Principle

The central concurrency rule used by the implementation is:

```text
Lock the wallet
    ↓
Check idempotency
    ↓
Validate balance
    ↓
Update wallet
    ↓
Record transaction
    ↓
Commit
```

This ordering ensures that idempotency and balance validation occur inside the same protected transaction flow.
