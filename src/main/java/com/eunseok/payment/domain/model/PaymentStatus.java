package com.eunseok.payment.domain.model;

// Allowed transitions:
// INIT -> AUTHORIZED | FAILED | CANCELED
// AUTHORIZED -> SETTLED | FAILED | CANCELED
// SETTLED -> REVERSED
public enum PaymentStatus {
    INIT,
    AUTHORIZED,
    SETTLED,
    FAILED,
    CANCELED,
    REVERSED;

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case INIT -> target == AUTHORIZED || target == FAILED || target == CANCELED;
            case AUTHORIZED -> target == SETTLED || target == FAILED || target == CANCELED;
            case SETTLED -> target == REVERSED;
            default -> false;
        };
    }
}
