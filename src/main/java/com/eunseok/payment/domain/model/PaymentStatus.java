package com.eunseok.payment.domain.model;
/*
 * Payment lifecycle actions:
 *
 * CREATE    - Create a payment record (initial state, no funds involved)
 * AUTHORIZE - Reserve funds from the customer's payment method
 * SETTLE    - Capture and finalize the reserved funds
 * CANCEL   - Cancel the payment before funds are settled
 * FAIL     - Mark the payment as failed
 * REVERSE  - Refund a settled payment
 */

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
