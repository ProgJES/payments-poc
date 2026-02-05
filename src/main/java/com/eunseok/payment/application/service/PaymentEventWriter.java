package com.eunseok.payment.application.service;

import com.eunseok.payment.domain.model.PaymentEventType;
import com.eunseok.payment.domain.model.PaymentStatus;
import com.eunseok.payment.infra.persistence.entity.PaymentEntity;
import com.eunseok.payment.infra.persistence.entity.PaymentEventEntity;
import com.eunseok.payment.infra.persistence.repository.PaymentEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentEventWriter {
    private final PaymentEventRepository paymentEventRepository;

    public void statusChanged(
            PaymentEntity payment,
            PaymentStatus oldStatus,
            String payloadJson
    ) {
        paymentEventRepository.save(
                PaymentEventEntity.stateChanged(
                        payment.getPaymentId(),
                        PaymentEventType.STATUS_CHANGED,
                        oldStatus,
                        payment.getStatus(),
                        payloadJson
                )
        );
    }

}
