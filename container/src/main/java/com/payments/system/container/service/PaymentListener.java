package com.payments.system.container.service;

import com.payments.system.container.model.PaymentCompletedEvent;
import com.payments.system.container.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.payments.system.container.config.PaymentSystemConfiguration.PAYMENTS_COMPLETED_TOPIC;

@Slf4j
@Component
public class PaymentListener {
    private final NotificationService notificationService;

    public PaymentListener(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = PAYMENTS_COMPLETED_TOPIC, groupId = "payment-service")
    public void consume(PaymentCompletedEvent event) {
        log.info("Receive payment completed event {}", event);
        notificationService.paymentSuccessful(event);
    }
}
