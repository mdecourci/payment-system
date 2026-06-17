package com.payments.system.container.model;

import java.util.UUID;

public record NotificationMessage(UUID paymentId, String recipient, String subject, String body) {
}