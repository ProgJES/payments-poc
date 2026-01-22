package com.eunseok.payment.api.dto;

import java.time.Instant;

public record GetPaymentResponse(
        String paymentId,
        String status,
        Long amount,
        String currency,
        Instant createdAt
) {
}
