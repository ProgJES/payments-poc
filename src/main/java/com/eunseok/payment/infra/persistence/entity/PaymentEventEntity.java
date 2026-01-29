package com.eunseok.payment.infra.persistence.entity;

import com.eunseok.payment.domain.model.PaymentEventType;
import com.eunseok.payment.domain.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;

import java.time.Instant;

@Entity
@Table(name = "payment_events")
@Getter
public class PaymentEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private PaymentEventType eventType;

    @Column(name = "from_status", length = 32)
    @Enumerated(EnumType.STRING)
    private PaymentStatus fromStatus;

    @Column(name = "to_status", length = 32)
    @Enumerated(EnumType.STRING)
    private PaymentStatus toStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentEventEntity() {}

    public static PaymentEventEntity paymentCreated(
            String paymentId,
            PaymentStatus toStatus,
            String payloadJson
    ) {

        var e = new PaymentEventEntity();
        e.paymentId = paymentId;
        e.eventType = PaymentEventType.PAYMENT_CREATED;
        e.fromStatus = null;
        e.toStatus = toStatus;
        e.payload = payloadJson;
        e.createdAt = Instant.now();

        return e;
    }
}

