package com.eunseok.payment.domain.model;

public enum PaymentEventType {
    PAYMENT_CREATED,
    STATUS_CHANGED,
    AUTHORIZED,
    CAPTURED,
    FAILED
}
