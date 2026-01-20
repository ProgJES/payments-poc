package com.eunseok.payment.infra.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true, length = 64)
    private String paymentId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentEntity() {}

    public static PaymentEntity createNew(
            String paymentId,
            String idempotencyKey,
            String paymentMethod,
            Long amount,
            String currency,
            String description
    ) {
        var now = Instant.now();

        var e = new PaymentEntity();
        e.paymentId = paymentId;
        e.idempotencyKey = idempotencyKey;
        e.paymentMethod = paymentMethod;
        e.amount = amount;
        e.currency = currency;
        e.status = "INIT";
        e.description = description;
        e.createdAt = now;
        e.updatedAt = now;

        return e;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getStatus() {
        return status;
    }

    public Long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
