package com.eunseok.payment.api.dto;

import com.eunseok.payment.domain.model.PaymentStatus;

import java.time.Instant;

public record CreatePaymentResponse(
        String paymentId,
        PaymentStatus stauts,
        Long amount,
        String currency,
        Instant createdAt
) {
}
