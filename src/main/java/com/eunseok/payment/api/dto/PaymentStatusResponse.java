package com.eunseok.payment.api.dto;

import com.eunseok.payment.domain.model.PaymentStatus;

import java.time.Instant;

public record PaymentStatusResponse(
        String paymentId,
        PaymentStatus status,
        Instant updatedAt
) {
}
