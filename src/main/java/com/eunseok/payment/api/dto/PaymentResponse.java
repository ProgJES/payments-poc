package com.eunseok.payment.api.dto;

import com.eunseok.payment.domain.model.PaymentStatus;

import java.time.Instant;

public record PaymentResponse(
        String paymentId,
        PaymentStatus status,
        Long amount,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {
}
