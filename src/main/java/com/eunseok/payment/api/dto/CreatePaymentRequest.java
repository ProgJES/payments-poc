package com.eunseok.payment.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(@NotNull @Min(1) Long amount,
                                   @NotBlank String currency,
                                   @NotBlank String paymentMethod,
                                   String description) {
}
