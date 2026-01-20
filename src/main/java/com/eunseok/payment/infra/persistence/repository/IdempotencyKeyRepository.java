package com.eunseok.payment.infra.persistence.repository;

import com.eunseok.payment.infra.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {
    Optional<IdempotencyKeyEntity> findByIdempotencyKey(String key);
}
