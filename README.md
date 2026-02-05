# Payment Service

A lightweight payment API designed with **idempotency**, **explicit state transitions**, and **event logging**.

This project focuses on building a reliable payment core similar to real-world payment systems.

---

## ✨ Key Features

- Idempotent payment creation (`POST /payments`)
- Explicit payment lifecycle with validated state transitions
- Event-based audit log (`payment_events`)
- Consistent error handling via global exception handler

---

## 🔄 Payment Lifecycle

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

---
## 📚 Documentation

- [Payment Lifecycle](docs/payment-lifecycle.md)
- [Idempotency Strategy](docs/idempotency-key.md)
