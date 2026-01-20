package com.eunseok.payment.infra.persistence.entity;

import com.eunseok.payment.domain.model.IdempotencyStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key")
        }
)
public class IdempotencyKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private IdempotencyStatus status; // IN_PROGRESS, SUCCEEDED, FAILED

    @Column(name = "response_code")
    private Integer responseCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdempotencyKeyEntity() {

    }

    public static IdempotencyKeyEntity createInProgress(String idempotencyKey, String requestHash) {
        var now = Instant.now();
        var e= new IdempotencyKeyEntity();

        e.idempotencyKey = idempotencyKey;
        e.requestHash = requestHash;
        e.status = IdempotencyStatus.IN_PROGRESS;
        e.createdAt = now;
        e.updatedAt = now;

        return e;
    }

    /* State Transactions */
    public void markSucceeded(int responseCode, String responseBody) {
        this.status = IdempotencyStatus.SUCCEEDED;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.updatedAt = Instant.now();
    }

    public void markFailed(int responseCode, String responseBody) {
        this.status = IdempotencyStatus.FAILED;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.updatedAt = Instant.now();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
