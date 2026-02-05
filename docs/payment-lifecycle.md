# Payment Lifecycle

This service models payment as a state machine.
Each transition is validated to prevent illegal flows.

---

## Why a State Machine?

Payments are not a single-step operation in real systems:
- authorization
- capture/settlement
- cancellation/failure
- reversal (refund-like)

A strict lifecycle provides:
- predictable behavior
- easier debugging
- auditability
- safer future integrations (gateways, ledger, etc.)

---

## States

### INIT
Payment record created (intent).
No external action has happened yet.

### AUTHORIZED
Funds are authorized (reserved) by a payment provider.
(Not yet captured)

### SETTLED
Funds are captured / confirmed.

### FAILED
Payment attempt failed (authorization/capture failed).

### CANCELED
Payment was canceled before settlement (user/system canceled).

### REVERSED
A settled payment was reversed (refund-like).  
In Phase 1, we treat this as "logical reversal".

---

## Transition Rules

```text
INIT
 ├──> AUTHORIZED
 │      ├──> SETTLED
 │      ├──> FAILED
 │      └──> CANCELED
 │
 ├──> FAILED
 └──> CANCELED

SETTLED
 └──> REVERSED
