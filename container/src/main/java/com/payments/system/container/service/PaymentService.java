package com.payments.system.container.service;

import com.payments.system.container.gateway.PaymentGateway;
import com.payments.system.container.model.*;
import com.payments.system.container.notification.NotificationService;
import com.payments.system.container.repository.IdempotencyRepository;
import com.payments.system.container.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.payments.system.container.config.PaymentSystemConfiguration.PAYMENTS_COMPLETED_TOPIC;

@Slf4j
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final FraudDetectionService fraudService;
    private final LedgerService ledgerService;
    private final NotificationService notificationService;
    private final PaymentGateway gateway;
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    public PaymentService(final PaymentRepository paymentRepository, final IdempotencyRepository idempotencyRepository, final FraudDetectionService fraudService, final LedgerService ledgerService, final NotificationService notificationService, final PaymentGateway gateway, final KafkaTemplate kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.fraudService = fraudService;
        this.ledgerService = ledgerService;
        this.notificationService = notificationService;
        this.gateway = gateway;
        this.kafkaTemplate = kafkaTemplate;
    }

    public PaymentResponse process(PaymentRequest request) {
        log.info("Processing request {}", request);
        var existing = idempotencyRepository.findById(request.idempotencyKey());

        if (existing.isPresent()) {
            var payment = paymentRepository.findById(existing.get().getPaymentId()).orElseThrow();
            return new PaymentResponse(payment.getId(), payment.getStatus().name(), "Duplicate request");
        }

        if (FraudStatus.CLEAN != fraudService.evaluate(request)) {
            throw new IllegalStateException("Fraud detected");
        }

        var payment = paymentRepository.save(new Payment(request.userId(), request.amount(), request.currency()));
        boolean success = gateway.charge(request);

        if (!success) {
            payment.markFailed();
            return new PaymentResponse(payment.getId(), "FAILED", "Payment failed");
        }

        payment.markSuccess();

        ledgerService.postPayment(payment.getId(), payment.getAmount());
        idempotencyRepository.save(new IdempotencyKey(request.idempotencyKey(), payment.getId()));
        log.info("Send payment {}", payment);

        kafkaTemplate.send(PAYMENTS_COMPLETED_TOPIC, new PaymentCompletedEvent(payment.getId(), payment.getUserId(), payment.getAmount()));
        notificationService.notifySuccess(payment);

        return new PaymentResponse(payment.getId(), "SUCCESS", "Payment processed");
    }
}