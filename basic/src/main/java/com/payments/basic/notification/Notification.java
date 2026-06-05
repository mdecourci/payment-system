package com.payments.basic.notification;

import com.payments.basic.model.NotificationChannel;
import com.payments.basic.model.NotificationEventType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * An immutable notification record.
 * Created once and never mutated — delivery attempts are tracked separately.
 */
public record Notification(String id, String customerId, String customerEmail, String customerPhone,
                           NotificationEventType eventType, NotificationChannel channel, String subject, String body,
                           Map<String, String> metadata,    // e.g. {"transactionId": "TXN-...", "amount": "99.99"}
                           LocalDateTime createdAt) {
    public Notification {
        id = id != null ? id : "NTF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    /**
     * Convenience constructor — ID and createdAt auto-generated.
     */
    public Notification(String customerId, String customerEmail, String customerPhone, NotificationEventType eventType, NotificationChannel channel, String subject, String body, Map<String, String> metadata) {
        this(null, customerId, customerEmail, customerPhone, eventType, channel, subject, body, metadata, null);
    }
}
