package com.eunseok.payment.api.dto;

import com.eunseok.payment.domain.model.PaymentStatus;

import java.time.Instant;

public record GetPaymentResponse(
        String paymentId,
        PaymentStatus status,
        Long amount,
        String currency,
        Instant createdAt
) {
}
