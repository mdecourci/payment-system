package com.payments.system.container.notification;

import com.payments.system.container.model.NotificationMessage;
import com.payments.system.container.model.Payment;
import com.payments.system.container.model.PaymentCompletedEvent;
import com.payments.system.container.model.PaymentNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final EmailProvider emailProvider;
    private final SmsProvider smsProvider;
    private final PushProvider pushProvider;

    public void paymentSuccessful(PaymentCompletedEvent paymentCompletedEvent) {
        var message = new NotificationMessage(paymentCompletedEvent.id(), paymentCompletedEvent.userId(), "Payment Successful", "Your paymentCompletedEvent was processed.");
        emailProvider.send(message);
        smsProvider.send(message);
        pushProvider.send(message);
    }

    public void notifySuccess(Payment payment) {
        var event = new PaymentNotificationEvent(payment.getId(), payment.getUserId(), payment.getStatus().name());

        log.info("Sending notification {}", event);

    }
}