package com.eunseok.payment.application.service;

import com.eunseok.payment.api.dto.CreatePaymentRequest;
import com.eunseok.payment.api.dto.CreatePaymentResponse;
import com.eunseok.payment.api.dto.GetPaymentResponse;
import com.eunseok.payment.api.dto.PaymentEventResponse;
import com.eunseok.payment.common.util.Strings;
import com.eunseok.payment.domain.model.IdempotencyStatus;
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
import java.util.*;

import static java.util.Objects.hash;
import static org.flywaydb.core.internal.util.JsonUtils.toJson;

@Service
@AllArgsConstructor
public class PaymentService {
    // Repositories
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentEventRepository paymentEventRepository;

    // Other services
    private final ObjectMapper objectMapper;


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
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(req);
        } catch (Exception e) {
            payloadJson = "{}";
        }
        paymentEventRepository.save(
                PaymentEventEntity.paymentCreated(
                        saved.getPaymentId(),
                        saved.getStatus(),
                        payloadJson
                )
        );

        // Generate response
        var response = new CreatePaymentResponse(
                saved.getPaymentId(),
                saved.getStatus(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getCreatedAt()
        );
        String responseJson = toJson(response);
        idem.markSucceeded(HttpStatus.CREATED.value(), responseJson);
        idempotencyKeyRepository.save(idem);

        return response;
    }

    @Transactional(readOnly = true)
    public GetPaymentResponse getPayment(String paymentId) {
        PaymentEntity payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + paymentId
                ));
        return new GetPaymentResponse(
                payment.getPaymentId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt()
        );
    }

    /* In  service use private functions */
    private String toJson(CreatePaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String hash(CreatePaymentRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
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
}
