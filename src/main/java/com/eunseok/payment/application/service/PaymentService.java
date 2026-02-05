package com.eunseok.payment.application.service;

import com.eunseok.payment.api.dto.*;
import com.eunseok.payment.common.util.Strings;
import com.eunseok.payment.domain.model.IdempotencyStatus;
import com.eunseok.payment.domain.model.PaymentEventType;
import com.eunseok.payment.domain.model.PaymentStatus;
import com.eunseok.payment.infra.persistence.entity.IdempotencyKeyEntity;
import com.eunseok.payment.infra.persistence.entity.PaymentEntity;
import com.eunseok.payment.infra.persistence.entity.PaymentEventEntity;
import com.eunseok.payment.infra.persistence.repository.IdempotencyKeyRepository;
import com.eunseok.payment.infra.persistence.repository.PaymentEventRepository;
import com.eunseok.payment.infra.persistence.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@AllArgsConstructor
public class PaymentService {
    // Repositories
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentEventRepository paymentEventRepository;

    // Other services
    private final ObjectMapper objectMapper;
    private final PaymentEventWriter paymentEventWriter;

    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest req, String idempotencyKey) {
        String key = Strings.normalizedOrGenerate(idempotencyKey);
        String requestHash = hash(req);
        boolean isNew = false;

        // Chek idempotency key (prevent concurrency)
        IdempotencyKeyEntity idem = idempotencyKeyRepository.findByIdempotencyKey(key).orElse(null);
        if (idem == null) {
            try {
                idem = idempotencyKeyRepository.save(IdempotencyKeyEntity.createInProgress(key, requestHash));
                isNew = true;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                idem = idempotencyKeyRepository.findByIdempotencyKey(key).orElseThrow(() -> e);
            }
        }

        // Check request hash with same idempotency key
        if (!idem.getRequestHash().equals(requestHash)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency-Key was resued with a different request payload"
            );
        }

        if (idem.getStatus() == IdempotencyStatus.SUCCEEDED) {
            return parseResponse(idem.getResponseBody());
        }

        if (idem.getStatus() == IdempotencyStatus.IN_PROGRESS && !isNew) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Request with the same Idempotency-Key is already in progress"
            );
        }

        if (idem.getStatus() == IdempotencyStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Previous request with this Idempotency-Key failed"
            );
        }

        // Create payment record
        PaymentEntity saved = paymentRepository.save(
                PaymentEntity.createNew(
                        generatePaymentId(),
                        key,
                        req.paymentMethod(),
                        req.amount(),
                        req.currency(),
                        req.description()
                )
        );

        // Create payment event log
        String payloadJson = safeJson(req);

        paymentEventRepository.save(
                PaymentEventEntity.paymentCreated(
                        saved.getPaymentId(),
                        saved.getStatus(),
                        payloadJson
                )
        );

        // Generate response
        CreatePaymentResponse response = new CreatePaymentResponse(
                saved.getPaymentId(),
                saved.getStatus(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getCreatedAt()
        );
        String responseJson = safeJson(response);
        idem.markSucceeded(HttpStatus.CREATED.value(), responseJson);
        idempotencyKeyRepository.save(idem);

        return response;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + paymentId
                ));
        return toResponse(payment);
    }


    private String hash(CreatePaymentRequest request) {
        try {
            String json = safeJson(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash request", e);
        }
    }

    private CreatePaymentResponse parseResponse(String json) {
        try {
            return objectMapper.readValue(json, CreatePaymentResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generatePaymentId() {
        return UUID.randomUUID().toString();
    }

    @Transactional(readOnly = true)
    public List<PaymentEventResponse> getPaymentEvents(String paymentId) {
        boolean exists = paymentRepository.existsByPaymentId(paymentId);
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: " + paymentId);
        }
        return paymentEventRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId)
                .stream()
                .map(e -> new PaymentEventResponse(
                        e.getEventType(),
                        e.getFromStatus(),
                        e.getToStatus(),
                        e.getPayload(),
                        e.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public PaymentResponse authorize(String paymentId) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
            return toResponse(payment);
        }

        PaymentStatus oldStatus = payment.getStatus();
        PaymentStatus newStatus = PaymentStatus.AUTHORIZED;
        payment.changeStatus(newStatus);

        String payloadJson = safeJson(Map.of(
                "action", "authorized",
                "at", Instant.now().toString()));

        paymentEventWriter.statusChanged(payment, oldStatus, payloadJson);

        return toResponse(payment);
    }

    public PaymentResponse settle(String paymentId) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() == PaymentStatus.SETTLED) {
            return toResponse(payment);
        }

        PaymentStatus oldStatus = payment.getStatus();
        PaymentStatus newStatus = PaymentStatus.SETTLED;
        payment.changeStatus(newStatus);

        String payloadJson = safeJson(Map.of(
           "action", "settle",
           "at", Instant.now()
        ));

        paymentEventWriter.statusChanged(payment, oldStatus, payloadJson);
        return toResponse(payment);
    }

    public PaymentResponse cancel(String paymentId) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return toResponse(payment);
        }

        PaymentStatus oldStatus = payment.getStatus();
        PaymentStatus newStatus = PaymentStatus.CANCELED;
        payment.changeStatus(newStatus);

        String payloadJson = safeJson(Map.of(
                "action", "canceled",
                "at", Instant.now()
        ));

        paymentEventWriter.statusChanged(payment, oldStatus, payloadJson);
        return toResponse(payment);
    }

    public PaymentResponse fail(String paymentId) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() == PaymentStatus.FAILED) {
            return toResponse(payment);
        }

        PaymentStatus oldStatus = payment.getStatus();
        PaymentStatus newStatus = PaymentStatus.FAILED;
        payment.changeStatus(newStatus);

        String payloadJson = safeJson(Map.of(
                "action", "failed",
                "at", Instant.now()
        ));

        paymentEventWriter.statusChanged(payment, oldStatus, payloadJson);
        return toResponse(payment);
    }

    public PaymentResponse reverse(String paymentId) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() == PaymentStatus.REVERSED) {
            return toResponse(payment);
        }

        PaymentStatus oldStatus = payment.getStatus();
        PaymentStatus newStatus = PaymentStatus.REVERSED;
        payment.changeStatus(newStatus);

        String payloadJson = safeJson(Map.of(
                "action", "reversed",
                "at", Instant.now()
        ));

        paymentEventWriter.statusChanged(payment, oldStatus, payloadJson);
        return toResponse(payment);
    }
    private PaymentResponse toResponse(PaymentEntity payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }


    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignore) {
            return "{}";
        }
    }
}
