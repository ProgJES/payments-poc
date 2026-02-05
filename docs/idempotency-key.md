# Idempotency Strategy

This document describes how idempotency is implemented in the Payment Service
to prevent duplicate payment creation when the same request is retried.

---

## What is Idempotency?

Idempotency guarantees that **the same request is processed only once**, even if
the client sends it multiple times.

This is critical for payment systems because:
- Users may click a button multiple times
- Network retries can occur
- Timeouts may cause clients to resend requests

Without idempotency, duplicate payments could be created.

---

## When Idempotency is Applied

Idempotency is applied to:

- `POST /payments`

This endpoint **creates state and side effects**, so it must be protected.

---

## Client Responsibility

### Idempotency-Key Header

Clients must send an `Idempotency-Key` header:
Rules:
- One key per **user intent**
- Retry the same request using the **same key**
- Do NOT reuse the same key for different payloads

Typical key formats:
- UUID / ULID
- `orderId-attemptNumber`

In a real system, the **calling service** (e.g. order-service) is responsible
for generating and managing this key.

---

## Database Model

Table: `idempotency_keys`

Purpose:
- Acts as a **concurrency lock**
- Stores request fingerprint and final response

Columns:

- `idempotency_key` (UNIQUE)
- `request_hash`
- `status` (`IN_PROGRESS`, `SUCCEEDED`, `FAILED`)
- `response_code`
- `response_body`
- `created_at`
- `updated_at`

---

## Request Hash

Before processing the request:
1. Serialize request body to JSON
2. Compute SHA-256 hash
3. Store hash in `request_hash`

Why:
- Detect reuse of the same Idempotency-Key with **different request payloads**
- Prevent accidental or malicious misuse

---

## Server-Side Flow

### Step 1: Normalize or Generate Key

- Read `Idempotency-Key` from header
- Optionally generate one if missing (policy decision)

---

### Step 2: Try to Create Idempotency Record

The server attempts to insert a new row:

- `status = IN_PROGRESS`
- `request_hash = computed hash`

This happens **before** creating the payment.

Why:
- The database unique constraint guarantees that only one request wins
- Acts as a concurrency gate

If insert fails due to unique constraint:
- Another request already inserted the row
- Fetch the existing record instead

---

### Step 3: Validate Request Hash

If stored `request_hash` ≠ current hash:
- Reject request
- Return `409 CONFLICT`

This means the same key was reused with a different request body.

---

### Step 4: Handle by Idempotency Status

#### Case: `SUCCEEDED`

- Return the stored response
- Same HTTP status
- Same response body

No new processing occurs.

---

#### Case: `IN_PROGRESS`

- Another request is currently being processed
- Return `409 CONFLICT` (Phase 1 policy)

---

#### Case: `FAILED`

- Previous attempt failed
- Return `409 CONFLICT` (Phase 1 policy)

---

### Step 5: Process Payment

Only when:
- Request hash matches
- Status is `IN_PROGRESS`
- This request is the creator of the record

Then:
- Create payment
- Persist payment event
- Build response

---

### Step 6: Finalize Idempotency Record

After successful processing:
- Update status to `SUCCEEDED`
- Store `response_code`
- Store `response_body`

All future retries will replay this response.

---

## Concurrency Example

User clicks "Pay" 5 times rapidly:

- All requests share the same Idempotency-Key
- Only one request successfully inserts `IN_PROGRESS`
- Others:
  - Read the existing row
  - See `IN_PROGRESS`
  - Are rejected with `409`

Once the first request commits:
- Status becomes `SUCCEEDED`
- Retries return the stored response

---

## HTTP Response Summary

| Scenario | Response |
|--------|----------|
| First valid request | 201 Created |
| Same key, same payload, already succeeded | Stored response |
| Same key, different payload | 409 Conflict |
| Same key, in progress | 409 Conflict |
| Previous attempt failed | 409 Conflict |

---

## Design Notes

- Database uniqueness is the source of truth
- No in-memory locks are required
- Idempotency works across multiple instances
- Cleanup / TTL is deferred to Phase 2
