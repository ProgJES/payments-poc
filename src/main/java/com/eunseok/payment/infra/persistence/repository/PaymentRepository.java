package com.eunseok.payment.infra.persistence.repository;

import com.eunseok.payment.infra.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByPaymentId(String paymentId);
    boolean existsByPaymentId(String paymentId);
}
