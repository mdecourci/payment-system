package com.payments.basic.notification;

import java.util.Map;

/**
 * Sealed hierarchy — each channel has its own specific payload shape.
 * The NotificationDispatcher switch-matches on these to call the right sender.
 */
public sealed interface ChannelPayload permits ChannelPayload.EmailPayload, ChannelPayload.SmsPayload, ChannelPayload.PushPayload, ChannelPayload.WebhookPayload {

    /**
     * HTML email with optional plain-text fallback
     */
    record EmailPayload(String to, String from, String subject, String htmlBody, String plainTextBody,
                        String replyTo) implements ChannelPayload {
    }

    /**
     * Short text message — max 160 chars for single SMS
     */
    record SmsPayload(String toPhoneNumber, String fromNumber, String message) implements ChannelPayload {
        public SmsPayload {
            if (message != null && message.length() > 320)
                throw new IllegalArgumentException("SMS message too long: " + message.length() + " chars");
        }
    }

    /**
     * Mobile push notification
     */
    record PushPayload(String deviceToken, String title, String body, Map<String, String> data,
                       // key-value extras sent to the device
                       String imageUrl) implements ChannelPayload {
    }

    /**
     * HTTP POST to a merchant-configured endpoint
     */
    record WebhookPayload(String url, String secret,       // HMAC-SHA256 signing secret
                          String eventType, Map<String, Object> payload) implements ChannelPayload {
    }
}
