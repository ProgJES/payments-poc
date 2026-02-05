package com.eunseok.payment.api.controller;

import com.eunseok.payment.api.dto.*;
import com.eunseok.payment.application.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /*
     * Create: Create a new payment intent (no money moved yet)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse create(
            @RequestHeader(value = "Idempotency-key", required = false) String idempotencyKey,
            @RequestBody @Valid CreatePaymentRequest request
            ) {

        return paymentService.createPayment(request, idempotencyKey);
    }
    /*
    * Authorize: Reserve funds from the payment method
    */
    @PostMapping("/{paymentId}/authorize")
    @ResponseStatus(HttpStatus.OK)
    public PaymentResponse authorize(@PathVariable String paymentId) {
        return paymentService.authorize(paymentId);
    }
    /*
     * Settle: Capture and finalize the authorized funds
     */
    @PostMapping("/{paymentId}/settle")
    @ResponseStatus(HttpStatus.OK)
    public PaymentResponse settle(@PathVariable String paymentId) {
        return paymentService.settle(paymentId);
    }
    /*
     * Cancel: Cancel the payment before settlement
     */
    @PostMapping("/{paymentId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public PaymentResponse cancel(@PathVariable String paymentId) {
        return paymentService.cancel(paymentId);
    }
    /*
     * Fail: Mark the payment as failed due to an error
     */
    @PostMapping("/{paymentId}/fail")
    @ResponseStatus(HttpStatus.OK)
    public PaymentResponse fail(@PathVariable String paymentId) {
        return paymentService.fail(paymentId);
    }
    /*
     * Reverse: Refund a settled payment (money is returned)
     */
    @PostMapping("/{paymentId}/reverse")
    @ResponseStatus(HttpStatus.OK)
    public PaymentResponse reverse(@PathVariable String paymentId) {
        return paymentService.reverse(paymentId);
    }
    /*
     * Current Payment Status
     */
    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable String paymentId) {
        return paymentService.getPayment(paymentId);
    }

    /*
     * Payment Event History
     */
    @GetMapping("/{paymentId}/events")
    public List<PaymentEventResponse> events(@PathVariable String paymentId) {
        return paymentService.getPaymentEvents(paymentId);
    }
}
