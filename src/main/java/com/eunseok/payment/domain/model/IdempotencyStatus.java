package com.eunseok.payment.domain.model;

public enum IdempotencyStatus {
    IN_PROGRESS,
    SUCCEEDED,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
