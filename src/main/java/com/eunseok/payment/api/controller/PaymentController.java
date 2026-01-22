package com.eunseok.payment.api.controller;

import com.eunseok.payment.api.dto.CreatePaymentRequest;
import com.eunseok.payment.api.dto.CreatePaymentResponse;
import com.eunseok.payment.api.dto.GetPaymentResponse;
import com.eunseok.payment.application.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse create(
            @RequestHeader(value = "Idempotency-key", required = false) String idempotencyKey,
            @RequestBody @Valid CreatePaymentRequest request
            ) {

        return paymentService.createPayment(request, idempotencyKey);
    }

    @GetMapping("/{paymentId}")
    public GetPaymentResponse get(@PathVariable String paymentId) {
        return paymentService.getPayment(paymentId);
    }
}
