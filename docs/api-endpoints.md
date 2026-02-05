# API Endpoints

This document describes the REST API endpoints provided by the Payment Service (Phase 1).

Base URL (local):
http://localhost:8080

Content-Type:
application/json

---

## Common Headers

### Idempotency-Key
Used only for payment creation to prevent duplicate requests.

Idempotency-Key: <string>

---

## Error Response Format

```json
{
  "code": "CONFLICT",
  "message": "Request with the same Idempotency-Key is already in progress"
}
```

---

## 1) Create Payment

POST /payments

Creates a new payment in INIT status.

Request Body:
```json
{
  "amount": 1000,
  "currency": "CAD",
  "paymentMethod": "CARD",
  "description": "smoke test"
}
```

Response (201):
```json
{
  "paymentId": "uuid",
  "status": "INIT",
  "amount": 1000,
  "currency": "CAD",
  "createdAt": "timestamp"
}
```

---

## 2) Get Payment

GET /payments/{paymentId}

Response:
```json
{
  "paymentId": "uuid",
  "status": "AUTHORIZED",
  "amount": 1000,
  "currency": "CAD",
  "createdAt": "timestamp"
}
```

---

## 3) Get Payment Events

GET /payments/{paymentId}/events

Response:
```json
[
  {
    "eventType": "PAYMENT_CREATED",
    "fromStatus": null,
    "toStatus": "INIT",
    "payload": "{}",
    "createdAt": "timestamp"
  }
]
```

---

## 4) Authorize Payment

POST /payments/{paymentId}/authorize

Moves payment from INIT to AUTHORIZED.

---

## 5) Settle Payment

POST /payments/{paymentId}/settle

Moves payment from AUTHORIZED to SETTLED.

---

## 6) Cancel Payment

POST /payments/{paymentId}/cancel

Moves payment from INIT or AUTHORIZED to CANCELED.

---

## 7) Fail Payment

POST /payments/{paymentId}/fail

Moves payment to FAILED.

---

## 8) Reverse Payment

POST /payments/{paymentId}/reverse

Moves payment from SETTLED to REVERSED.

