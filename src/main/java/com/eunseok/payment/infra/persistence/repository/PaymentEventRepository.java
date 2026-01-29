package com.eunseok.payment.infra.persistence.repository;

import com.eunseok.payment.infra.persistence.entity.PaymentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEventRepository extends JpaRepository<PaymentEventEntity, Long> {
    List<PaymentEventEntity> findByPaymentIdOrderByCreatedAtAsc(String paymentId);
}
