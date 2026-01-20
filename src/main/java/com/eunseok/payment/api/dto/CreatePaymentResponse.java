package com.eunseok.payment.api.dto;

import java.time.Instant;

public record CreatePaymentResponse(
        String paymentId,
        String stauts,
        Long amount,
        String currency,
        Instant createdAt
) {
}
