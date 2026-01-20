# payments-poc
Experimental payment system POC exploring payment flows, idempotency handling, and async gateway communication.

## What this explores
This project investigates how payment authorization and capture workflows can be modeled with idempotent APIs and async processing for external vendor notifications.

## Scope
- Authorization → Capture → Refund flows
- Idempotency key enforcement for duplicate requests
- Async vendor communication
- External gateway abstraction

## Domain Concepts
- Authorization vs Capture
- Retry + Idempotency at API boundary
- Gateway vs Processor
- Async vendor communication

## Notes
- Not focused on production infrastructure
- Domain and API behavior exploration only
