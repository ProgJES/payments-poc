package com.eunseok.payment.api.dto;

import com.eunseok.payment.domain.model.PaymentEventType;
import com.eunseok.payment.domain.model.PaymentStatus;

import java.time.Instant;

public record PaymentEventResponse(
        PaymentEventType eventType,
        PaymentStatus fromStatus,
        PaymentStatus toStatus,
        String payload,
        Instant createdAt
) {
}
