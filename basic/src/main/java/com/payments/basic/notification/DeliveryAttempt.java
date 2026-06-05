package com.payments.basic.notification;

import com.payments.basic.model.NotificationChannel;
import com.payments.basic.model.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks a single delivery attempt — a notification may be retried,
 * so there can be multiple DeliveryAttempts per Notification.
 */
public record DeliveryAttempt(String id, String notificationId, NotificationChannel channel, NotificationStatus status,
                              String providerMessageId,   // e.g. SES message ID, Twilio SID
                              String errorMessage, int attemptNumber, LocalDateTime attemptedAt) {
    public DeliveryAttempt {
        id = id != null ? id : "ATT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        attemptedAt = attemptedAt != null ? attemptedAt : LocalDateTime.now();
    }

    public DeliveryAttempt(String notificationId, NotificationChannel channel, NotificationStatus status, String providerMessageId, String errorMessage, int attemptNumber) {
        this(null, notificationId, channel, status, providerMessageId, errorMessage, attemptNumber, null);
    }

    public boolean isSuccessful() {
        return status == NotificationStatus.SENT || status == NotificationStatus.DELIVERED;
    }
}
